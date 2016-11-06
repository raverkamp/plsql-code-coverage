package spinat.codecoverage.cover;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import spinat.plsqlparser.Ast;
import spinat.plsqlparser.ParseException;
import spinat.plsqlparser.Parser;
import spinat.plsqlparser.Range;
import spinat.plsqlparser.ScanException;
import spinat.plsqlparser.Scanner;
import spinat.plsqlparser.Seq;
import spinat.plsqlparser.Token;

// class to extract the needed statement information
public class StatementExtractor {

    // instead of parameters to methods or global variables place the needed information
    // in instance variables
    final String bodySource;
    final String specSource;
    final Ast.PackageSpec specAst;
    final Ast.PackageBody bodyAst;

    private StatementExtractor(String specSource, String bodySource, Ast.PackageSpec specAst,
            Ast.PackageBody bodyAst) {
        this.specSource = specSource;
        this.bodySource = bodySource;
        this.specAst = specAst;
        this.bodyAst = bodyAst;
    }

    // return a statement extractor or null, if null place in msg[] a message
    public static StatementExtractor create(String specSource, String bodySource, String[] msg) {
        final Ast.PackageSpec specAst;
        if (msg.length != 1) {
            throw new RuntimeException("message array must have length 1");
        }
        Parser p = new Parser();
        try {
            ArrayList<Token> ts = Scanner.scanAll(specSource);
            Seq se = relevant(ts);
            specAst = p.paPackageSpec(se).v;
        } catch (ParseException ex) {
            msg[0] = "Error when parsing package specification: " + ex.getMessage();
            return null;
        } catch (ScanException ex) {
            msg[0] = "Error when parsing package specification: " + ex.getMessage();
            return null;
        }

        final Ast.PackageBody bodyAst;
        try {
            ArrayList<Token> ts = Scanner.scanAll(bodySource);
            Seq se = relevant(ts);
            bodyAst = p.pPackageBody.pa(se).v;
        } catch (ParseException ex) {
            msg[0] = "Error when parsing package body: " + ex.getMessage();
            return null;
        } catch (ScanException ex) {
            msg[0] = "Error when parsing package body: " + ex.getMessage();
            return null;
        }
        msg[0] = null;
        return new StatementExtractor(specSource, bodySource, specAst, bodyAst);
    }

    private static Seq relevant(ArrayList<Token> ts) {
        ArrayList<Token> x = new ArrayList<>();
        for (Token t : ts) {
            if (Scanner.isRelevant(t)) {
                x.add(t);
            }
        }
        return new Seq(x);
    }

    List<String> extractRestrictReferences() {
        ArrayList<String> l = new ArrayList<>();
        for (Ast.Declaration decl : this.specAst.declarations) {
            if (decl instanceof Ast.PragmaRestrictReferences) {
                Ast.PragmaRestrictReferences pr = (Ast.PragmaRestrictReferences) decl;
                if (!pr.default_) {
                    l.add(pr.name.val);
                }
            }
        }
        return l;
    }

    public static class SqlAttrExprAndRange {

        public final String attribute;
        public final Range range;

        public SqlAttrExprAndRange(String attribute, Range range) {
            this.attribute = attribute;
            this.range = range;
        }
    }

    // for instrumenting the code we need to know the ranges of the statements
    // and the beginning of the procedure section
    // this class carries this information
    public static class ExtractionResult {

        // the ranges of all statements
        public final List<Range> statementRanges;
        // the ranges of all statements that change an sql% attribute, dml
        public final List<Range> sqlAttrChangers;
        // all sql%... expressions
        public final List<SqlAttrExprAndRange> sqlAttrExpressions;
        public final int firstProcedurePosition;

        public ExtractionResult(List<Range> statementRanges,
                List<Range> sqlAttrChangers,
                List<SqlAttrExprAndRange> sqlAttrExpressions,
                int firstProcedurePosition) {
            this.statementRanges = statementRanges;
            this.sqlAttrChangers = sqlAttrChangers;
            this.sqlAttrExpressions = sqlAttrExpressions;
            this.firstProcedurePosition = firstProcedurePosition;
        }
    }

    // extract the required information
    // but skip the excluded procedures
    public ExtractionResult extract(Set<String> excludedProcedures) {

        Ast.PackageBody pb = this.bodyAst;
   //     ArrayList<Ast.Statement> l = new ArrayList<>();
   //     ArrayList<Ast.Expression> el = new ArrayList<>();
        ExtractionWalker w = new ExtractionWalker();
        w.excludedProcs = excludedProcedures;
        w.walkPackageBody(pb);
   
        ArrayList<Range> ranges = new ArrayList<>();
        ArrayList<Range> sql_attr_changer_ranges = new ArrayList<>();

        for (Ast.Statement stm : w.statementList) {
            ranges.add(new Range(stm.getStart(), stm.getEnd()));
            if (stm instanceof Ast.SqlStatement
                    || stm instanceof Ast.ExecuteImmediateDML
                    || stm instanceof Ast.ExecuteImmediateInto) {
                sql_attr_changer_ranges.add(new Range(stm.getStart(), stm.getEnd()));
            }
        }
        ArrayList<SqlAttrExprAndRange> sel = new ArrayList<>();
        for (Ast.Expression e : w.expressionList) {
            if (e instanceof Ast.SqlAttribute) {
                Ast.SqlAttribute a = (Ast.SqlAttribute) e;
                SqlAttrExprAndRange s = new SqlAttrExprAndRange(a.attribute.name(), new Range(a.getStart(), a.getEnd()));
                sel.add(s);
            }
        }

        int first_proc_pos = findFirstProc(pb);

        return new ExtractionResult(ranges, sql_attr_changer_ranges, sel, first_proc_pos);
    }

    // find the first procedure or function definition inside a package 
    // body, this needed to place code
    public int findFirstProc(Ast.PackageBody pb) {
        for (Ast.Declaration d : pb.declarations) {
            if (d instanceof Ast.FunctionDeclaration
                    || d instanceof Ast.FunctionDefinition
                    || d instanceof Ast.ProcedureDeclaration
                    || d instanceof Ast.ProcedureDefinition) {
                return d.getStart();
            }
        }
        throw new RuntimeException("no proc or fun");
    }
    
    // extract all top level procdures/functions
    public List<ProcedureAndRange> getProcedureRanges() {
        Map<String, ArrayList<String>> pp = getPublicPrograms();
        ArrayList<ProcedureAndRange> res = new ArrayList<>();
        for (Ast.Declaration decl : this.bodyAst.declarations) {
            if (decl instanceof Ast.ProcedureDefinition) {
                Ast.ProcedureDefinition pd = (Ast.ProcedureDefinition) decl;
                final boolean publik;
                if (pp.containsKey(pd.procedureheading.name.val)) {
                    String a = getParameterString(decl);
                    publik = pp.get(pd.procedureheading.name.val).contains(a);

                } else {
                    publik = false;
                }
                ProcedureAndRange pr = new ProcedureAndRange(
                        pd.procedureheading.name.val, publik,
                        new Range(decl.getStart(), decl.getEnd()));
                res.add(pr);
            } else if (decl instanceof Ast.FunctionDefinition) {
                Ast.FunctionDefinition fd = (Ast.FunctionDefinition) decl;
                final boolean publik;
                if (pp.containsKey(fd.functionheading.name.val)) {
                    String a = getParameterString(decl);
                    publik = pp.get(fd.functionheading.name.val).contains(a);

                } else {
                    publik = false;
                }

                ProcedureAndRange pr = new ProcedureAndRange(
                        fd.functionheading.name.val, publik,
                        new Range(decl.getStart(), decl.getEnd()));
                res.add(pr);
            }
        }
        return Collections.unmodifiableList(res);
    }

    public Range bodyStatementsRange(Ast.PackageBody b) {
        if (b.statements != null && b.statements.size() > 0) {
            return new Range(b.statements.get(0).getStart(),
                    b.statements.get(b.statements.size() - 1).getEnd());
        } else {
            return null;
        }
    }

    // extract the public procedures and functions from the package spec
    // return a Map, the key is the procedure name and the values
    // is alist of all signatures, the signatures (i.e. parameter) are rendered as string
    // the form of the renndering does not matter, we just have to be able to
    // identify the procedures in the body
    Map<String, ArrayList<String>> getPublicPrograms() {
        HashMap<String, ArrayList<String>> res = new HashMap<>();

        Ast.PackageSpec ps = this.specAst;

        for (Ast.Declaration d : ps.declarations) {
            if (d instanceof Ast.ProcedureDeclaration
                    || d instanceof Ast.FunctionDeclaration) {
                final String name;
                if (d instanceof Ast.ProcedureDeclaration) {
                    name = ((Ast.ProcedureDeclaration) d).procedureheading.name.val;
                } else {
                    name = ((Ast.FunctionDeclaration) d).functionheading.name.val;
                }
                if (!res.containsKey(name)) {
                    res.put(name, new ArrayList<String>());
                }
                res.get(name).add(getParameterString(d));
            }
        }
        return res;
    }

    String getParameterString(Ast.Declaration decl) {
        StringBuilder sb = new StringBuilder();
        final List<Ast.Parameter> params;

        if (decl instanceof Ast.ProcedureDeclaration) {
            Ast.ProcedureDeclaration p = (Ast.ProcedureDeclaration) decl;
            params = p.procedureheading.parameters;
            sb.append(p.procedureheading.name.val).append("/");
        } else if (decl instanceof Ast.ProcedureDefinition) {
            Ast.ProcedureDefinition p = (Ast.ProcedureDefinition) decl;
            params = p.procedureheading.parameters;
            sb.append(p.procedureheading.name.val).append("/");
        } else if (decl instanceof Ast.FunctionDeclaration) {
            Ast.FunctionDeclaration p = (Ast.FunctionDeclaration) decl;
            params = p.functionheading.parameters;
            sb.append(p.functionheading.name.val).append("/");
        } else if (decl instanceof Ast.FunctionDefinition) {
            Ast.FunctionDefinition p = (Ast.FunctionDefinition) decl;
            params = p.functionheading.parameters;
            sb.append(p.functionheading.name.val).append("/");
        } else {
            return "";
        }

        for (Ast.Parameter param : params) {
            sb.append(param.ident.val);
            if (param.parammode != null) {
                sb.append("/").append(param.parammode.paramModeType.name());
                sb.append("/").append(param.parammode.nocopy);
            } else {
                sb.append("/").append("IN");
                sb.append("/").append(false);
            }
            sb.append("/").append(dataTypeToString(param.datatype));
        }

        return sb.toString();
    }

    String identsString(List<Ast.Ident> li) {
        StringBuilder sb = new StringBuilder();
        for (Ast.Ident i : li) {
            sb.append("/").append(i.val);
        }
        return sb.toString();
    }

    String dataTypeToString(Ast.DataType dt) {
        if (dt instanceof Ast.NamedType) {
            return identsString(((Ast.NamedType) dt).idents);
        }
        if (dt instanceof Ast.RowType) {
            return identsString(((Ast.RowType) dt).idents) + "%rowtype";
        }
        if (dt instanceof Ast.VarType) {
            return identsString(((Ast.VarType) dt).idents) + "%type";
        }
        if (dt instanceof Ast.ParameterizedType) {
            Ast.ParameterizedType t = (Ast.ParameterizedType) dt;
            return t.ident.val + "(" + t.var1 + "," + t.var2 + ")";
        }
        return dt.getClass().getSimpleName();
    }

}
