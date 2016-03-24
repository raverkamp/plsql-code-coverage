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
        ArrayList<Ast.Statement> l = new ArrayList<>();
        ArrayList<Ast.Expression> el = new ArrayList<>();
        getPackageBodyStatementsAndExpressions(pb, l, el, excludedProcedures);
        ArrayList<Range> ranges = new ArrayList<>();
        ArrayList<Range> sql_attr_changer_ranges = new ArrayList<>();

        for (Ast.Statement stm : l) {
            ranges.add(new Range(stm.getStart(), stm.getEnd()));
            if (stm instanceof Ast.SqlStatement
                    || stm instanceof Ast.ExecuteImmediateDML
                    || stm instanceof Ast.ExecuteImmediateInto) {
                sql_attr_changer_ranges.add(new Range(stm.getStart(), stm.getEnd()));
            }
        }
        ArrayList<SqlAttrExprAndRange> sel = new ArrayList<>();
        for (Ast.Expression e : el) {
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

    // the code walker, excluded procedures will be skipped
    void getPackageBodyStatementsAndExpressions(Ast.PackageBody b,
            ArrayList<Ast.Statement> l,
            ArrayList<Ast.Expression> el,
            Set<String> excludedProcedures) {
        declStatements(b.declarations, l, excludedProcedures, el);
        if (b.statements != null) {
            checkStatements(b.statements, l, el);
            if (b.exceptionBlock != null) {
                for (Ast.ExceptionHandler ec : b.exceptionBlock.handlers) {
                    checkStatements(ec.statements, l, el);
                }
                if (b.exceptionBlock.othershandler != null) {
                    checkStatements(b.exceptionBlock.othershandler, l, el);
                }
            }
        }
    }

    void procedureStatements(Ast.ProcedureDefinition b, ArrayList<Ast.Statement> l, ArrayList<Ast.Expression> el) {
        declStatements(b.block.declarations, l, null, el);
        if (b.block != null) {
            blockStatements(b.block, l, el);
        }
    }

    void functionStatements(Ast.FunctionDefinition b, ArrayList<Ast.Statement> l, ArrayList<Ast.Expression> el) {
        declStatements(b.block.declarations, l, null, el);
        if (b.block != null) {
            blockStatements(b.block, l, el);
        }
    }

    void arrayStatements(List<Ast.Statement> stml, ArrayList<Ast.Statement> l, ArrayList<Ast.Expression> el) {
        checkStatements(stml, l, el);
    }

    void declStatements(List<Ast.Declaration> d, ArrayList<Ast.Statement> l, Set<String> exclude, ArrayList<Ast.Expression> el) {
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
                checkDecl(decl, l, el);
            }
        }
    }

    void checkActualParams(List<Ast.ActualParam> ps, ArrayList<Ast.Expression> l) {
        for (Ast.ActualParam p : ps) {
            checkExpression(p.expr, l);
        }
    }

    void checkCallParts(List<Ast.CallPart> cps, ArrayList<Ast.Expression> l) {
        for (Ast.CallPart cp : cps) {
            if (cp instanceof Ast.Component) {
                // nix
            } else if (cp instanceof Ast.CallOrIndexOp) {
                Ast.CallOrIndexOp o = (Ast.CallOrIndexOp) cp;
                checkActualParams(o.params, l);
            }
        }
    }

    // extract the sql%something expressions from an expression
    // not finished !
    void checkExpression(Ast.Expression expr, ArrayList<Ast.Expression> l) {
        if (expr == null) {
            return;
        }
        if (expr instanceof Ast.SqlAttribute) {
            l.add((Ast.SqlAttribute) expr);
        } else if (expr instanceof Ast.OrExpr) {
            for (Ast.Expression e : ((Ast.OrExpr) expr).exprs) {
                checkExpression(e, l);
            }
        } else if (expr instanceof Ast.AndExpr) {
            for (Ast.Expression e : ((Ast.AndExpr) expr).exprs) {
                checkExpression(e, l);
            }
        } else if (expr instanceof Ast.CompareExpr) {
            Ast.CompareExpr e = (Ast.CompareExpr) expr;
            checkExpression(e.expr1, l);
            checkExpression(e.expr2, l);
        } else if (expr instanceof Ast.NotExpr) {
            Ast.NotExpr e = (Ast.NotExpr) expr;
            checkExpression(e.expr, l);
        } else if (expr instanceof Ast.ParenExpr) {
            Ast.ParenExpr e = (Ast.ParenExpr) expr;
            checkExpression(e.expr, l);
        } else if (expr instanceof Ast.BinopExpression) {
            Ast.BinopExpression e = (Ast.BinopExpression) expr;
            checkExpression(e.expr1, l);
            checkExpression(e.expr2, l);
        } else if (expr instanceof Ast.CaseBoolExpression) {
            Ast.CaseBoolExpression e = (Ast.CaseBoolExpression) expr;
            checkExpression(e.default_, l);
            for (Ast.CaseExpressionPart o : e.cases) {
                checkExpression(o.cond, l);
                checkExpression(o.result, l);
            }
        } else if (expr instanceof Ast.CaseMatchExpression) {
            Ast.CaseMatchExpression e = (Ast.CaseMatchExpression) expr;
            checkExpression(e.default_, l);
            for (Ast.CaseExpressionPart o : e.matches) {
                checkExpression(o.cond, l);
                checkExpression(o.result, l);
            }
        } else if (expr instanceof Ast.CastExpression) {
            Ast.CastExpression e = (Ast.CastExpression) expr;
            checkExpression(e.expr, l);
        } else if (expr instanceof Ast.ExtractDatePart) {
            Ast.ExtractDatePart e = (Ast.ExtractDatePart) expr;
            checkExpression(e.expr, l);
        } else if (expr instanceof Ast.InExpression) {
            Ast.InExpression e = (Ast.InExpression) expr;
            checkExpression(e.expr, l);
            for (Ast.Expression es : e.set) {
                checkExpression(es, l);
            }
        } else if (expr instanceof Ast.IsNullExpr) {
            Ast.IsNullExpr e = (Ast.IsNullExpr) expr;
            checkExpression(e.expr, l);
        } else if (expr instanceof Ast.LValue) {
            // yes! a(sql%rowcount) := true   .... sick
            Ast.LValue e = (Ast.LValue) expr;
            checkCallParts(e.callparts, l);
        } else if (expr instanceof Ast.LikeExpression) {
            Ast.LikeExpression e = (Ast.LikeExpression) expr;
            checkExpression(e.escape, l);
            checkExpression(e.expr1, l);
            checkExpression(e.expr2, l);
        } else if (expr instanceof Ast.MultisetExpr) {
            Ast.MultisetExpr e = (Ast.MultisetExpr) expr;
            checkExpression(e.e1, l);
            checkExpression(e.e2, l);
        } else if (expr instanceof Ast.NewExpression) {
            Ast.NewExpression e = (Ast.NewExpression) expr;
            checkCallParts(e.callParts, l);
        } else if (expr instanceof Ast.UnaryMinusExpression) {
            Ast.UnaryMinusExpression e = (Ast.UnaryMinusExpression) expr;
            checkExpression(e.expr, l);
        } else if (expr instanceof Ast.UnaryPlusExpression) {
            Ast.UnaryPlusExpression e = (Ast.UnaryPlusExpression) expr;
            checkExpression(e.expr, l);
        } else if (expr instanceof Ast.VarOrCallExpression) {
            Ast.VarOrCallExpression e = (Ast.VarOrCallExpression) expr;
            checkCallParts(e.callparts, l);
        } else if (expr instanceof Ast.CString
                || expr instanceof Ast.CBool
                || expr instanceof Ast.CNumber
                || expr instanceof Ast.CDate
                || expr instanceof Ast.CNull
                || expr instanceof Ast.DollarDollar
                || expr instanceof Ast.CursorAttribute
                || expr instanceof Ast.CInterval) {
            // nothing to do                           
        } else {
            throw new RuntimeException("missing check for expression type " + expr.getClass());
        }
    }

    void checkDecl(Ast.Declaration d, ArrayList<Ast.Statement> l, ArrayList<Ast.Expression> el) {
        if (d instanceof Ast.ProcedureDefinition) {
            Ast.ProcedureDefinition def = (Ast.ProcedureDefinition) d;
            blockStatements(def.block, l, el);

        }
        if (d instanceof Ast.FunctionDefinition) {
            Ast.FunctionDefinition def = (Ast.FunctionDefinition) d;
            blockStatements(def.block, l, el);

        }
        if (d instanceof Ast.VariableDeclaration) {
            Ast.VariableDeclaration vd = (Ast.VariableDeclaration) d;
            checkExpression(vd.default_, el);
        }
    }

    void checkStatement(Ast.Statement s, ArrayList<Ast.Statement> l, ArrayList<Ast.Expression> el) {
        if (s == null) {
            return;
        }
        l.add(s);

        if (s instanceof Ast.BlockStatement) {
            Ast.BlockStatement bs = (Ast.BlockStatement) s;

            blockStatements(bs.block, l, el);
        } else if (s instanceof Ast.IfStatement) {
            Ast.IfStatement ifs = (Ast.IfStatement) s;
            for (Ast.ExprAndStatements it : ifs.branches) {
                checkStatements(it.statements, l, el);
                checkExpression(it.expr, el);
            }
            if (ifs.elsebranch != null) {
                checkStatements(ifs.elsebranch, l, el);
            }
        } else if (s instanceof Ast.CaseCondStatement) {
            Ast.CaseCondStatement cs = (Ast.CaseCondStatement) s;
            for (Ast.ExprAndStatements ct : cs.branches) {
                checkStatements(ct.statements, l, el);
                checkExpression(ct.expr, el);
            }
            if (cs.defaultbranch != null) {
                checkStatements(cs.defaultbranch, l, el);
            }
        } else if (s instanceof Ast.CaseMatchStatement) {
            Ast.CaseMatchStatement cs = (Ast.CaseMatchStatement) s;
            for (Ast.ExprAndStatements ct : cs.branches) {
                checkStatements(ct.statements, l, el);
            }
            if (cs.defaultbranch != null) {
                checkStatements(cs.defaultbranch, l, el);
            }
        } else if (s instanceof Ast.BasicLoopStatement) {
            Ast.BasicLoopStatement sl = (Ast.BasicLoopStatement) s;
            checkStatements(sl.statements, l, el);
        } else if (s instanceof Ast.WhileLoopStatement) {
            Ast.WhileLoopStatement wl = (Ast.WhileLoopStatement) s;
            checkStatements(wl.statements, l, el);
            checkExpression(wl.condition, el);
        } else if (s instanceof Ast.FromToLoopStatement) {
            Ast.FromToLoopStatement fs = (Ast.FromToLoopStatement) s;
            checkStatements(fs.statements, l, el);
            checkExpression(fs.from, el);
            checkExpression(fs.to, el);
        } else if (s instanceof Ast.CursorLoopStatement) {
            Ast.CursorLoopStatement fs = (Ast.CursorLoopStatement) s;
            checkStatements(fs.statements, l, el);
        } else if (s instanceof Ast.SelectLoopStatement) {
            Ast.SelectLoopStatement fs = (Ast.SelectLoopStatement) s;
            checkStatements(fs.statements, l, el);
        } else if (s instanceof Ast.Assignment) {
            Ast.Assignment as = (Ast.Assignment) s;
            checkExpression(as.expression, el);
            // check for callparts
        } else if (s instanceof Ast.ReturnStatement) {
            Ast.ReturnStatement x = (Ast.ReturnStatement) s;
            checkExpression(x.expr, el);
        } else if (s instanceof Ast.ExitStatement) {
            Ast.ExitStatement x = (Ast.ExitStatement) s;
            checkExpression(x.condition, el);
        } else if (s instanceof Ast.ProcedureCall) {
            Ast.ProcedureCall pc = (Ast.ProcedureCall) s;
            for (Ast.CallPart cp : pc.callparts) {
                if (cp instanceof Ast.CallOrIndexOp) {
                    for (Ast.ActualParam ap : ((Ast.CallOrIndexOp) cp).params) {
                        checkExpression(ap.expr, el);
                    }
                }
            }
        } else if (s instanceof Ast.ExecuteImmediateDML) {
            // fixme
        } else if (s instanceof Ast.ForAllStatement) {
            Ast.ForAllStatement fa = (Ast.ForAllStatement) s;
            if (fa.bounds instanceof Ast.FromToBounds) {
                Ast.FromToBounds ftb = (Ast.FromToBounds) fa.bounds;
                checkExpression(ftb.from, el);
                checkExpression(ftb.to, el);
            } else if (fa.bounds instanceof Ast.ValuesBounds) {
                checkExpression(((Ast.ValuesBounds) fa.bounds).collection, el);
            } else if (fa.bounds instanceof Ast.IndicesBounds) {
                Ast.IndicesBounds ib = (Ast.IndicesBounds) fa.bounds;
                checkExpression(ib.idx_collection, el);
                checkExpression(ib.lower, el);
                checkExpression(ib.upper, el);
            }
        } else // no simple else clause to make sire we catch everything
        if (s instanceof Ast.NullStatement
                || s instanceof Ast.Rollback
                || s instanceof Ast.ContinueStatement
                || s instanceof Ast.SqlStatement
                || s instanceof Ast.CloseStatement
                || s instanceof Ast.OpenDynamicRefCursorStatement
                || s instanceof Ast.OpenFixedCursorStatement
                || s instanceof Ast.OpenStaticRefCursorStatement
                || s instanceof Ast.FetchStatement
                || s instanceof Ast.CloseStatement) {
            // nothing to do
        } else {
            throw new RuntimeException("no check for statement type " + s.getClass());
        }
    }

    void checkStatements(List<Ast.Statement> s, ArrayList<Ast.Statement> l, ArrayList<Ast.Expression> el) {
        for (Ast.Statement stm : s) {
            if (stm == null) {
            }
            checkStatement(stm, l, el);
        }
    }

    private void blockStatements(Ast.Block block, ArrayList<Ast.Statement> l, ArrayList<Ast.Expression> el) {
        declStatements(block.declarations, l, null, el);
        checkStatements(block.statements, l, el);
        if (block.exceptionBlock != null) {
            for (Ast.ExceptionHandler ec : block.exceptionBlock.handlers) {
                checkStatements(ec.statements, l, el);
            }
            if (block.exceptionBlock.othershandler != null) {
                checkStatements(block.exceptionBlock.othershandler, l, el);
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
