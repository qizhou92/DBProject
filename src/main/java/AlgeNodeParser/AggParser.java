package AlgeNodeParser;

import AlgeNode.AlgeNode;
import AlgeNode.AggNode;
import com.microsoft.z3.Context;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.AggregateCall;
import org.apache.calcite.rel.logical.LogicalAggregate;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexNode;

import java.util.ArrayList;
import java.util.List;


public class AggParser extends AlgeNodeParser{
    public AlgeNode constructRelNode(RelNode input, Context z3Context){
        LogicalAggregate aggregate = (LogicalAggregate) input;
        AlgeNode inputNode = AlgeNodeParserPair.constructAlgeNode(aggregate.getInput(),z3Context);
        List<Integer> groupByList = aggregate.getGroupSet().asList();
        ArrayList<RexNode> outputExpr = new ArrayList<RexNode>();
        for(int i=0;i<groupByList.size();i++){
            int index = groupByList.get(i);
            RelDataType type = inputNode.getOutputExpr().get(index).getType();
            RexNode column = new EqRexFieldAccess(i,type,0);
            outputExpr.add(column);
        }
        List<AggregateCall> aggregateCallList = aggregate.getAggCallList();
        for(int i=0;i<aggregateCallList.size();i++){
            AggregateCall aggregateCall = aggregateCallList.get(i);
            RexNode column = new EqRexFieldAccess(i+groupByList.size(),aggregateCall.getType(),0);
            outputExpr.add(column);
        }
        AlgeNode aggNode = new AggNode(groupByList,aggregateCallList,outputExpr,inputNode,z3Context);
        return aggNode;
    }
}
