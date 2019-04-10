package SymbolicRexNode;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.sql.SqlKind;

import java.util.List;

public class ArithmeticPredicate extends RexNodeBase {
    public ArithmeticPredicate(List<SymbolicColumn> inputs, RexNode node, Context z3Context){
        super(inputs,node,z3Context);
        buildExpr(node);
    }
    private void buildExpr(RexNode node){
        RexCall callNode = (RexCall) node;
        RexNode left = callNode.getOperands().get(0);
        RexNode right = callNode.getOperands().get(1);

        RexNodeBase leftConverter = RexNodeConverter.getRexConstrains(inputs, left, z3Context);
        RexNodeBase rightConverter = RexNodeConverter.getRexConstrains(inputs, right, z3Context);

        this.assignConstraints.addAll(leftConverter.getAssignConstrains());
        this.assignConstraints.addAll(rightConverter.getAssignConstrains());

        Expr outputValue = buildCompareResult(leftConverter.getOutputValue(), rightConverter.getOutputValue(), callNode);
        BoolExpr outputNull = (BoolExpr) z3Context.mkOr(leftConverter.getOutputNull(), rightConverter.getOutputNull()).simplify();

        this.output = new SymbolicColumn(outputValue,outputNull,z3Context);
    }
    private BoolExpr buildCompareResult(Expr leftExpr, Expr rightExpr, RexCall node){
        SqlKind sqlKind = node.getKind();
        switch (sqlKind){
            case LESS_THAN:
                return  z3Context.mkLt((ArithExpr) leftExpr,(ArithExpr) rightExpr);
            case LESS_THAN_OR_EQUAL:
                return z3Context.mkLe((ArithExpr) leftExpr,(ArithExpr) rightExpr);
            case GREATER_THAN:
                return z3Context.mkGt((ArithExpr) leftExpr,(ArithExpr) rightExpr);
            case GREATER_THAN_OR_EQUAL:
                return z3Context.mkGe((ArithExpr) leftExpr,(ArithExpr) rightExpr);
            case EQUALS:
                return z3Context.mkEq(leftExpr,rightExpr);
            case NOT_EQUALS:
                return z3Context.mkNot(z3Context.mkEq(leftExpr,rightExpr));
            default:{
            }

        }
        return null;
    }
}
