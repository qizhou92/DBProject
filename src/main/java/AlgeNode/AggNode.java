package AlgeNode;

import SymbolicRexNode.SymbolicColumn;
import Z3Helper.z3Utility;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.sun.org.apache.xpath.internal.operations.Bool;
import org.apache.calcite.rel.core.AggregateCall;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.sql.SqlAggFunction;
import org.apache.calcite.sql.SqlKind;

import java.util.*;

public class AggNode extends AlgeNode{
    List<Integer> groupByList;
    List<AggregateCall> aggregateCallList;
    public AggNode(List<Integer> groupByList, List<AggregateCall> aggregateCallList, List<RexNode> outputExpr, AlgeNode  input, Context z3Context){
        List<AlgeNode> inputs = new ArrayList<>();
        inputs.add(input);
        List<RelDataType> types = new ArrayList<>();
        for(Integer index:groupByList){
            types.add(input.getOutputExpr().get(index).getType());
        }
        for(AggregateCall aggregateCall: aggregateCallList){
            types.add(aggregateCall.getType());
        }

        setBasicFields(z3Context,types,inputs,outputExpr,new HashSet<>());

        this.groupByList = groupByList;
        this.aggregateCallList = aggregateCallList;
    }

    public AlgeNode getInput(){
        return this.getInputs().get(0);
    }

    @Override
    public String toString() {
        String result = "Agg Node: \n Group By:(";
        for(int i=0;i<groupByList.size();i++){
            result=result+groupByList.get(i)+",";
        }
        result=result+")\n AggCallSet: (";
        for(int i=0;i<aggregateCallList.size();i++){
            result=result+aggregateCallList.get(i).toString()+",";
        }
        result=result+")\n" + super.toString();
        return result;
    }


    public boolean isCartesianEq(AlgeNode node) {
        if(node instanceof AggNode){
            AggNode aggNode = (AggNode) node;
            if(groupByEq(aggNode)){
                matchSymbolicInputs(aggNode);
                return checkSymbolicCondition(node);
            }
        }
        return false;
    }


    private void matchSymbolicInputs(AggNode node){
        List<SymbolicAggPair> symbolicAggPairs = new ArrayList<>();
        this.constructSymbolicInputs(symbolicAggPairs);
        node.constructSymbolicInputs(symbolicAggPairs);

    }

    public void constructSymbolicInputs(List<SymbolicAggPair> symbolicAggPairs){
        this.inputSymbolicColumns = new ArrayList<>();
        inputSymbolicColumns.addAll(this.getSymbolicGroupByColumns());
        inputSymbolicColumns.addAll(this.constructSymbolicAggCalls(symbolicAggPairs));
    }

    private List<SymbolicColumn> constructSymbolicAggCalls(List<SymbolicAggPair> symbolicAggPairs){
        List<SymbolicColumn> symbolicAggCalls = new ArrayList<>();
        AlgeNode input = this.getInput();
        for(int i=0;i<this.aggregateCallList.size();i++){
            AggregateCall aggCall = this.aggregateCallList.get(i);
            boolean findMatch = false;
            for(int j=0;j<symbolicAggPairs.size();j++){
                SymbolicAggPair thePair = symbolicAggPairs.get(j);
                if(thePair.isEqualAggCall(input,aggCall)){
                    findMatch = true;
                    symbolicAggCalls.add(thePair.getSymbolicColumn());
                    break;
                }
            }
            if(!findMatch){
                SymbolicColumn newColumn = SymbolicColumn.mkNewSymbolicColumn(z3Context,aggCall.getType());
                symbolicAggCalls.add(newColumn);
                symbolicAggPairs.add(new SymbolicAggPair(aggCall,newColumn,input));
            }
        }
        return symbolicAggCalls;
    }

    private boolean groupByEq(AggNode node){
        AlgeNode input1 = this.getInput();
        AlgeNode input2 = node.getInput();
        if(input1.isCartesianEq(input2)){
            return groupByColumnBijective(node);
        }
        return false;
    }


    public List<SymbolicColumn> getSymbolicGroupByColumns(){
        List<SymbolicColumn> symbolicGroupByColumns = new ArrayList<>();
        for(int i=0;i<this.groupByList.size();i++){
            int index = this.groupByList.get(i);
            symbolicGroupByColumns.add(this.getInput().getSymbolicOutput().get(index));
        }
        return symbolicGroupByColumns;
    }

    private boolean groupByColumnBijective(AggNode node){
        List<SymbolicColumn> groupByColumn1 = this.getSymbolicGroupByColumns();
        List<SymbolicColumn> groupByColumn2 = node.getSymbolicGroupByColumns();
        if(groupByColumn1.isEmpty()&&groupByColumn2.isEmpty()){
            return true;
        }
        BoolExpr env = constructFreshEnv(node);
        //System.out.println("Environment:"+env);
        return groupBySymbolicDecide(env,groupByColumn1,groupByColumn2);

    }
    private boolean groupBySymbolicDecide(BoolExpr env,List<SymbolicColumn> groupByColumn1,List<SymbolicColumn> groupByColumn2){
        BoolExpr eq1 = freshEq(groupByColumn1);
        BoolExpr eq2 = freshEq(groupByColumn2);
        BoolExpr Eq1DecideEq2 = z3Context.mkAnd(env,eq1,z3Context.mkNot(eq2));
        if(z3Utility.isUnat(Eq1DecideEq2,z3Context)){
            BoolExpr Eq2DecideEq1 = z3Context.mkAnd(env,eq2,z3Context.mkNot(eq1));
            return z3Utility.isUnat(Eq2DecideEq1,z3Context);
        }
        return false;
    }
    private BoolExpr freshEq(List<SymbolicColumn> groupByColumn){
        List<SymbolicColumn> freshColumn = SymbolicColumn.constructFreshColumns(groupByColumn,z3Context);
        BoolExpr[] columnEqs1 = new BoolExpr[groupByColumn.size()];
        for(int i=0;i<freshColumn.size();i++){
            columnEqs1[i] = z3Utility.symbolicColumnEq(groupByColumn.get(i),freshColumn.get(i),z3Context);
        }
        return z3Context.mkAnd(columnEqs1);
    }

    private BoolExpr constructFreshEnv(AggNode node){
        this.setInputTupleConstraints();
        node.setInputTupleConstraints();
        BoolExpr inputConstraints1 =z3Utility.mkAnd(this.getInputTupleConstraints(),z3Context);
        BoolExpr inputConstraints2 =z3Utility.mkAnd(node.getInputTupleConstraints(),z3Context);
        BoolExpr freshInputAssign1 = (BoolExpr) z3Utility.constructFreshExpr(inputConstraints1,z3Context);
        BoolExpr freshInputAssign2 = (BoolExpr) z3Utility.constructFreshExpr(inputConstraints2,z3Context);
        BoolExpr env = z3Context.mkAnd(inputConstraints1,inputConstraints2,freshInputAssign1,freshInputAssign2);
        return env;
    }

}
