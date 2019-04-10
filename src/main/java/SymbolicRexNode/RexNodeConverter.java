package SymbolicRexNode;

import com.microsoft.z3.Context;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexLiteral;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.sql.SqlKind;

import java.util.Arrays;
import java.util.List;

public class RexNodeConverter {
    static public SqlKind[] logicSQL = {SqlKind.AND,SqlKind.OR,SqlKind.NOT,SqlKind.IN};
    static public SqlKind[] isNullSQL = {SqlKind.IS_NULL,SqlKind.IS_NOT_NULL};
    static public SqlKind[] arithmeticCompareSQL = {SqlKind.LESS_THAN,SqlKind.LESS_THAN_OR_EQUAL,SqlKind.GREATER_THAN,
            SqlKind.GREATER_THAN_OR_EQUAL,
            SqlKind.EQUALS, SqlKind.NOT_EQUALS};
    static public SqlKind[] arithmeticSQL = {SqlKind.PLUS,SqlKind.MINUS,SqlKind.TIMES,SqlKind.DIVIDE};
    public static RexNodeBase getRexConstrains(List<SymbolicColumn> inputs, RexNode node, Context z3Context){
        if(node instanceof RexLiteral){
            RexNodeBase newBase = (new Constant(inputs,node,z3Context));
            return newBase;
        }
        if(node instanceof RexCall){
            RexNodeBase newBase = getRexCallConstrains(inputs,node,z3Context);
            return newBase;
        }
        if(node instanceof RexInputRef){
            RexNodeBase newBase =new RexInputRefConstrains(inputs,node,z3Context);
            return newBase;
        }
        return null;
    }
    private static RexNodeBase getRexCallConstrains(List<SymbolicColumn> inputs, RexNode node, Context z3Context){
        RexCall callNode = (RexCall) node;
        if(callNode.isA(SqlKind.CASE)){
            return (new CaseNode(inputs,node,z3Context));
        }
        if(callNode.isA(SqlKind.OTHER_FUNCTION)){
            return (new UserDeFun(inputs,node,z3Context));
        }
        if(callNode.isA(Arrays.asList(logicSQL))){
            return (new BoolPredicate(inputs,node,z3Context));
        }
        if(callNode.isA(Arrays.asList(isNullSQL))){
            return (new NullPredicate(inputs,node,z3Context));
        }
        if(callNode.isA(Arrays.asList(arithmeticCompareSQL))){
            return (new ArithmeticPredicate(inputs,node,z3Context));
        }
        if(callNode.isA(Arrays.asList(arithmeticSQL))){
            return (new ArithmeticExpr(inputs,node,z3Context));
        }
        if(callNode.isA(SqlKind.CAST)){
            RexNode operand = callNode.getOperands().get(0);
            return getRexConstrains(inputs,operand,z3Context);
        }
        if(callNode.isA(SqlKind.IS_TRUE)){
            RexNode operand = callNode.getOperands().get(0);
            return getRexConstrains(inputs,operand,z3Context);
        }
        //System.out.println("end call");
        System.out.println("This callNode has not be handled:"+callNode.getClass());
        System.out.println(callNode.toString());
        return (new DumpRexNode(inputs,node,z3Context));
    }


}
