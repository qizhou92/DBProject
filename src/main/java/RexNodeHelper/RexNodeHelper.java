package RexNodeHelper;

import AlgeNode.AlgeNode;
import AlgeNodeParser.EqRexFieldAccess;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexLiteral;
import org.apache.calcite.rex.RexNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RexNodeHelper {
    static public RexNode substitute(RexNode input, List<RexNode> outputExpr){
        if(input instanceof RexInputRef){
            RexInputRef inputRef = (RexInputRef) input;
            return outputExpr.get(inputRef.getIndex());
        }
        if(input instanceof RexCall){
            RexCall rexCall = (RexCall) input;
            List<RexNode> operands = rexCall.getOperands();
            List<RexNode> newOperands = new ArrayList<RexNode>();
            for(int i=0;i<operands.size();i++){
                newOperands.add(substitute(operands.get(i),outputExpr));
            }
            return rexCall.clone(rexCall.getType(),newOperands);
        }
        if(input instanceof RexLiteral){
            return input;
        }

        System.out.println("The current rex node is not handle for substitute:"+input.getClass().toString());
        return input;
    }
    static public RexNode addTableIndex(RexNode input,int offset){
        if(input instanceof EqRexFieldAccess){
            EqRexFieldAccess inputRef = (EqRexFieldAccess) input;
            EqRexFieldAccess newInputRef = new EqRexFieldAccess(inputRef.getFieldIndex(),inputRef.getType(),inputRef.getTableIndex()+offset);
            return newInputRef;
        }
        if(input instanceof RexCall){
            RexCall rexCall = (RexCall) input;
            List<RexNode> operands = rexCall.getOperands();
            List<RexNode> newOperands = new ArrayList<RexNode>();
            for(int i=0;i<operands.size();i++){
                newOperands.add(addTableIndex(operands.get(i),offset));
            }
            return rexCall.clone(rexCall.getType(),newOperands);
        }
        if(input instanceof RexLiteral){
            return input;
        }

        System.out.println("The current rex node is not handle for addTableIndex:"+input.getClass().toString());
        return input;
    }

    static public RexNode updateTableIndex(RexNode input, Map<Integer,Integer> oldToNew,Map<Integer,Integer> oldToOffset){
        if(input instanceof EqRexFieldAccess){
            EqRexFieldAccess inputRef = (EqRexFieldAccess) input;
            int tableIndex = oldToNew.get(inputRef.getTableIndex());
            int offset = oldToOffset.get(inputRef.getTableIndex());
            EqRexFieldAccess newInputRef = new EqRexFieldAccess(inputRef.getFieldIndex(),inputRef.getType(),tableIndex);
            newInputRef.setOffset(offset);
            return newInputRef;
        }
        if(input instanceof RexCall){
            RexCall rexCall = (RexCall) input;
            List<RexNode> operands = rexCall.getOperands();
            List<RexNode> newOperands = new ArrayList<RexNode>();
            for(int i=0;i<operands.size();i++){
                newOperands.add(updateTableIndex(operands.get(i),oldToNew,oldToOffset));
            }
            return rexCall.clone(rexCall.getType(),newOperands);
        }
        if(input instanceof RexLiteral){
            return input;
        }

        System.out.println("The current rex node is not handle for addTableIndex:"+input.getClass().toString());
        return input;
    }

}
