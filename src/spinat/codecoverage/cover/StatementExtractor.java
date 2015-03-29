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
import spinat.plsqlparser.ScanException;
import spinat.plsqlparser.Scanner;
import spinat.plsqlparser.Seq;
import spinat.plsqlparser.Token;

public class StatementExtractor {

    public static class EitherExtractorOrMessage {

        private final String msg;
        private final StatementExtractor stex;

        public EitherExtractorOrMessage(String msg) {
            this.msg = msg;
            this.stex = null;
        }

        public EitherExtractorOrMessage(StatementExtractor stex) {
            this.msg = null;
            this.stex = stex;
        }

        public boolean isExtractor() {
            return this.stex != null;
        }

        public String getMessage() {
            if (isExtractor()) {
                throw new RuntimeException("BUG");
            } else {
                return this.msg;
            }
        }

        public StatementExtractor getExtractor() {
            if (isExtractor()) {
                return this.stex;
            } else {
                throw new RuntimeException("BUG");
            }
        }

    }

    public static class ExtractionResult {

        public final List<Range> statementRanges;
        public final int firstProcedurePosition;

        public ExtractionResult(List<Range> statementRanges,
                int firstProcedurePosition) {
            this.statementRanges = statementRanges;
            this.firstProcedurePosition = firstProcedurePosition;
        }
    }

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

    public static EitherExtractorOrMessage create(String specSource, String bodySource) {
        final Ast.PackageSpec specAst;

        Parser p = new Parser();
        try {
            ArrayList<Token> ts = Scanner.scanAll(specSource);
            Seq se = relevant(ts);
            specAst = p.paPackageSpec(se).v;
        } catch (ParseException ex) {
            return new EitherExtractorOrMessage(
                    "Error when parsing package specification: " + ex.getMessage());
        } catch (ScanException ex) {
            return new EitherExtractorOrMessage(
                    "Error when parsing package specification: " + ex.getMessage());
        }

        final Ast.PackageBody bodyAst;
        try {
            ArrayList<Token> ts = Scanner.scanAll(bodySource);
            Seq se = relevant(ts);
            bodyAst = p.pPackageBody.pa(se).v;
        } catch (ParseException ex) {
            return new EitherExtractorOrMessage(
                    "Error when parsing package body: " + ex.getMessage());
        } catch (ScanException ex) {
            return new EitherExtractorOrMessage(
                    "Error when parsing package body: " + ex.getMessage());
        }
        return new EitherExtractorOrMessage(
                new StatementExtractor(specSource, bodySource, specAst, bodyAst));
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

    public ExtractionResult extract(Set<String> excludedProcedures) {

        Ast.PackageBody pb = this.bodyAst;
        ArrayList<Ast.Statement> l = new ArrayList<>();
        getPackageBodyStatements(pb, l, excludedProcedures);
        ArrayList<Range> ranges = new ArrayList<>();
        for (Ast.Statement stm : l) {
            ranges.add(new Range(stm.getStart(), stm.getEnd()));
        }

        int x = findFirstProc(pb);

        return new ExtractionResult(ranges, x);
    }

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

    void getPackageBodyStatements(Ast.PackageBody b, ArrayList<Ast.Statement> l, Set<String> excludedProcedures) {
        declStatements(b.declarations, l, excludedProcedures);
        if (b.statements != null) {
            checkStatements(b.statements, l);
            if (b.exceptionBlock != null) {
                for (Ast.ExceptionHandler ec : b.exceptionBlock.handlers) {
                    checkStatements(ec.statements, l);
                }
                if (b.exceptionBlock.othershandler != null) {
                    checkStatements(b.exceptionBlock.othershandler, l);
                }
            }
        }
    }

    void procedureStatements(Ast.ProcedureDefinition b, ArrayList<Ast.Statement> l) {
        declStatements(b.block.declarations, l, null);
        if (b.block != null) {
            blockStatements(b.block, l);
        }
    }

    void functionStatements(Ast.FunctionDefinition b, ArrayList<Ast.Statement> l) {
        declStatements(b.block.declarations, l, null);
        if (b.block != null) {
            blockStatements(b.block, l);
        }
    }

    void arrayStatements(List<Ast.Statement> stml, ArrayList<Ast.Statement> l) {
        checkStatements(stml, l);
    }

    void declStatements(List<Ast.Declaration> d, ArrayList<Ast.Statement> l, Set<String> exclude) {
        if (d != null) {
            for (Ast.Declaration decl : d) {
                if (exclude != null) {
                    if (decl instanceof Ast.ProcedureDefinition) {
                        String name = ((Ast.ProcedureDefinition) decl).procedureheading.name.val;
                        if (exclude.contains(name)) {
                            continue;
                        }
                    }
                    if (decl instanceof Ast.FunctionDefinition) {
                        String name = ((Ast.FunctionDefinition) decl).functionheading.name.val;
                        if (exclude.contains(name)) {
                            continue;
                        }
                    }
                }
                checkDecl(decl, l);
            }
        }
    }

    void checkDecl(Ast.Declaration d, ArrayList<Ast.Statement> l) {
        if (d instanceof Ast.ProcedureDefinition) {
            Ast.ProcedureDefinition def = (Ast.ProcedureDefinition) d;
            blockStatements(def.block, l);

        }
        if (d instanceof Ast.FunctionDefinition) {
            Ast.FunctionDefinition def = (Ast.FunctionDefinition) d;
            blockStatements(def.block, l);

        }
    }

    void checkStatement(Ast.Statement s, ArrayList<Ast.Statement> l) {
        if (s == null) {
            return;
        }
        l.add(s);

        if (s instanceof Ast.BlockStatement) {
            Ast.BlockStatement bs = (Ast.BlockStatement) s;

            blockStatements(bs.block, l);
        } else if (s instanceof Ast.IfStatement) {
            Ast.IfStatement ifs = (Ast.IfStatement) s;
            for (Ast.ExprAndStatements it : ifs.branches) {// int i = 0; i < ifs.ifs.length; i++) {
                checkStatements(it.statements, l);
            }
            if (ifs.elsebranch != null) {
                checkStatements(ifs.elsebranch, l);
            }
        } else if (s instanceof Ast.CaseCondStatement) {
            Ast.CaseCondStatement cs = (Ast.CaseCondStatement) s;
            for (Ast.ExprAndStatements ct : cs.branches) {
                checkStatements(ct.statements, l);
            }
            if (cs.defaultbranch != null) {
                checkStatements(cs.defaultbranch, l);
            }
        } else if (s instanceof Ast.CaseMatchStatement) {
            Ast.CaseMatchStatement cs = (Ast.CaseMatchStatement) s;
            for (Ast.ExprAndStatements ct : cs.branches) {
                checkStatements(ct.statements, l);
            }
            if (cs.defaultbranch != null) {
                checkStatements(cs.defaultbranch, l);
            }
        } else if (s instanceof Ast.BasicLoopStatement) {
            Ast.BasicLoopStatement sl = (Ast.BasicLoopStatement) s;
            checkStatements(sl.statements, l);
        } else if (s instanceof Ast.WhileLoopStatement) {
            Ast.WhileLoopStatement wl = (Ast.WhileLoopStatement) s;
            checkStatements(wl.statements, l);
        } else if (s instanceof Ast.FromToLoopStatement) {
            Ast.FromToLoopStatement fs = (Ast.FromToLoopStatement) s;
            checkStatements(fs.statements, l);
        } else if (s instanceof Ast.CursorLoopStatement) {
            Ast.CursorLoopStatement fs = (Ast.CursorLoopStatement) s;
            checkStatements(fs.statements, l);
        } else if (s instanceof Ast.SelectLoopStatement) {
            Ast.SelectLoopStatement fs = (Ast.SelectLoopStatement) s;
            checkStatements(fs.statements, l);
        } else {
            // no child statements
        }
    }

    void checkStatements(List<Ast.Statement> s, ArrayList<Ast.Statement> l) {
        for (Ast.Statement stm : s) {
            if (stm == null) {
            }
            checkStatement(stm, l);
        }
    }

    private void blockStatements(Ast.Block block, ArrayList<Ast.Statement> l) {
        declStatements(block.declarations, l, null);
        checkStatements(block.statements, l);
        if (block.exceptionBlock != null) {
            for (Ast.ExceptionHandler ec : block.exceptionBlock.handlers) {
                checkStatements(ec.statements, l);
            }
            if (block.exceptionBlock.othershandler != null) {
                checkStatements(block.exceptionBlock.othershandler, l);
            }
        }
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
