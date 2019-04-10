package AlgeNodeParser;

import AlgeNode.AlgeNode;
import AlgeNode.SPJNode;
import RexNodeHelper.RexNodeHelper;
import com.microsoft.z3.Context;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.JoinRelType;
import org.apache.calcite.rel.logical.LogicalJoin;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InnerJoinParser extends AlgeNodeParser{
    public AlgeNode constructRelNode(RelNode input, Context z3Context){
        LogicalJoin joinNode = (LogicalJoin) input;
        if(joinNode.getJoinType() == JoinRelType.INNER) {
            AlgeNode leftNode = AlgeNodeParserPair.constructAlgeNode(joinNode.getLeft(),z3Context);
            AlgeNode rightNode = AlgeNodeParserPair.constructAlgeNode(joinNode.getRight(),z3Context);

            // build new output expr;
            List<RexNode> newOutputExpr = new ArrayList<>();

            newOutputExpr.addAll(leftNode.getOutputExpr());

            List<RexNode> rightOutputExpr = rightNode.getOutputExpr();
            int offSize = numberOfSubTable(leftNode);
            for(int i=0;i<rightOutputExpr.size();i++){
                newOutputExpr.add(RexNodeHelper.addTableIndex(rightOutputExpr.get(i),offSize));
            }

            // build new condition;
            Set<RexNode> newCondition = new HashSet<RexNode>();
            newCondition.addAll(leftNode.getConditions());

            Set<RexNode> rightConditions = rightNode.getConditions();
            for(RexNode rightCondition:rightConditions){
                newCondition.add(RexNodeHelper.addTableIndex(rightCondition,offSize));
            }
            RexNode newJoinCondition = RexNodeHelper.substitute(joinNode.getCondition(),newOutputExpr);
            newCondition.add(newJoinCondition);

            // getInput tables
            List<AlgeNode> inputs = new ArrayList<AlgeNode>();
            addInputs(leftNode,inputs);
            addInputs(rightNode,inputs);


            AlgeNode newSPJ = new SPJNode(newOutputExpr,newCondition,inputs,z3Context);
            return newSPJ;
        }
        else{
            System.out.println("it has to be inner join");
            System.exit(1);
        }
        return null;
    }

    private int numberOfSubTable(AlgeNode child){
        if(child instanceof SPJNode){
            return child.getInputs().size();
        }
        else{
            return 1;
        }
    }

    private void addInputs(AlgeNode child,List<AlgeNode> inputs){
        if(child instanceof SPJNode){
            inputs.addAll(child.getInputs());
        }else{
            child.resetOutputExpr();
            child.resetCondition();
            inputs.add(child);
        }
    }
}
