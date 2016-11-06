package spinat.codecoverage.cover;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import spinat.plsqlparser.Ast;
import spinat.plsqlparser.CodeWalker;

public class ExtractionWalker extends CodeWalker {
    
    public ArrayList<Ast.Expression> expressionList = new ArrayList<>();
    public ArrayList<Ast.Statement> statementList = new ArrayList<>();
    
    public Set<String> excludedProcs = new HashSet<>();
    
    @Override
    public void walkExpression(Ast.Expression expr) {
        if (expr instanceof Ast.SqlAttribute) {
            expressionList.add((Ast.SqlAttribute) expr);
        } else {
            super.walkExpression(expr);
        }
    }

    @Override
    public void walkStatement(Ast.Statement stmt) {
        this.statementList.add(stmt);
        super.walkStatement(stmt);
    }
    
    @Override
    public void walkDeclaration(Ast.Declaration decl) {
        if (excludedProcs != null) {
            if (decl instanceof Ast.ProcedureDefinition) {
                String name = ((Ast.ProcedureDefinition) decl).procedureheading.name.val;
                if (excludedProcs.contains(name)) {
                    return;
                }
            }
            if (decl instanceof Ast.FunctionDefinition) {
                String name = ((Ast.FunctionDefinition) decl).functionheading.name.val;
                if (excludedProcs.contains(name)) {
                    return;
                }
            }
        }
        super.walkDeclaration(decl);
    }
    
}
