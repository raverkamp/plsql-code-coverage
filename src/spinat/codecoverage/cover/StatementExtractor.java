package spinat.codecoverage.cover;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import spinat.plsqlparser.Ast;
import spinat.plsqlparser.Parser;
import spinat.plsqlparser.Res;
import spinat.plsqlparser.Scanner;
import spinat.plsqlparser.Seq;
import spinat.plsqlparser.Token;

public class StatementExtractor {

    public static class ExtractionResult {

        public final List<Range> statementRanges;
        public final int firstProcedurePosition;

        public ExtractionResult(List<Range> statementRanges,
                int firstProcedurePosition) {
            this.statementRanges = statementRanges;
            this.firstProcedurePosition = firstProcedurePosition;
        }
    }

    public static Seq relevant(ArrayList<Token> ts) {
        ArrayList<Token> x = new ArrayList<>();
        for (Token t : ts) {
            if (Scanner.isRelevant(t)) {
                x.add(t);
            }
        }
        return new Seq(x);
    }

    List<String> extractRestrictReferences(String s) {
        Parser p = new Parser();
        ArrayList<Token> ts = Scanner.scanAll(s);
        Seq se = relevant(ts);
        Res<Ast.PackageSpec> r = p.paPackageSpec(se);
        Ast.PackageSpec spec = r.v;
        List<String> l = new ArrayList<>();
        for (Ast.Declaration decl : spec.declarations) {
            if (decl instanceof Ast.PragmaRestrictReferences) {
                Ast.PragmaRestrictReferences pr = (Ast.PragmaRestrictReferences) decl;
                if (!pr.default_) {
                    l.add(pr.name.val);
                }
            }
        }
        return l;
    }

    public ExtractionResult extract(String s, Set<String> excludedProcedures) {
        Parser p = new Parser();
        ArrayList<Token> ts = Scanner.scanAll(s);
        Seq se = relevant(ts);
        Res<Ast.PackageBody> r = p.pPackageBody.pa(se);
        Ast.PackageBody pb = r.v;
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
    public List<ProcedureAndRange> getProcedureRanges(Ast.PackageBody b) {
        ArrayList<ProcedureAndRange> res = new ArrayList<>();
        for (Ast.Declaration decl : b.declarations) {
            if (decl instanceof Ast.ProcedureDefinition) {
                Ast.ProcedureDefinition pd = (Ast.ProcedureDefinition) decl;
                ProcedureAndRange pr = new ProcedureAndRange(pd.procedureheading.name.val, "p", new Range(decl.getStart(), decl.getEnd()));
                res.add(pr);
            } else if (decl instanceof Ast.FunctionDefinition) {
                Ast.FunctionDefinition fd = (Ast.FunctionDefinition) decl;
                ProcedureAndRange pr = new ProcedureAndRange(fd.functionheading.name.val, "p", new Range(decl.getStart(), decl.getEnd()));
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
    
    public List<ProcedureAndRange> getProcedureRanges(String s) {
        Parser p = new Parser();
        ArrayList<Token> ts = Scanner.scanAll(s);
        Seq se = relevant(ts);
        Res<Ast.PackageBody> r = p.pPackageBody.pa(se);
        Ast.PackageBody pb = r.v;
        return getProcedureRanges(pb);
    }

}
