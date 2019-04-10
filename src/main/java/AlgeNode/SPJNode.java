package AlgeNode;

import RexNodeHelper.RexNodeHelper;
import SymbolicRexNode.SymbolicColumn;
import Z3Helper.z3Utility;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexNode;

import java.util.*;

public class SPJNode extends AlgeNode{

    public SPJNode(List<RexNode> outputExpr,Set<RexNode> conditions, List<AlgeNode> inputs, Context z3Context){
        List<RelDataType> types = new ArrayList<>();
        for(AlgeNode input:inputs){
            for(RelDataType type:input.getInputTypes()){
                types.add(type);
            }
        }
        setBasicFields(z3Context,types,inputs,outputExpr,conditions);
    }

    @Override
    public String toString() {
        String result = "SPJ Node: "+"\n";
        return result+super.toString();
    }
    public boolean isCartesianEq(AlgeNode node) {
        if(node instanceof SPJNode){
            //System.out.println("before input matches");
             Map<Integer,Integer> inputMatches =checkInputMatch((SPJNode)node);
             //System.out.println("finish input matches");
             if(inputMatches!=null){
                 matchSymbolicInputs((SPJNode)node,inputMatches);
                 return checkSymbolicCondition(node);
             }
        }
        return false;
    }

    private void matchSymbolicInputs(SPJNode node,Map<Integer,Integer> inputMatches){
        List<Integer> newOrder1 = new ArrayList<>();
        List<Integer> newOrder2 = new ArrayList<>();
        for(Map.Entry<Integer,Integer> pairs:inputMatches.entrySet()){
            newOrder1.add(pairs.getKey());
            newOrder2.add(pairs.getValue());
        }
        this.constructSymbolicInputs(newOrder1);
        node.constructSymbolicInputs(newOrder2);
    }


    private void constructSymbolicInputs(List<Integer> newOrders){
        this.updateOrderOfInputs(newOrders);
        this.inputSymbolicColumns = new ArrayList<>();
        for(int i=0;i<inputs.size();i++){
            this.inputSymbolicColumns.addAll(inputs.get(i).getSymbolicOutput());
        }
    }

    private void updateOrderOfInputs(List<Integer> newOrders){
        Map<Integer,Integer> oldToNew = new HashMap<>();
        Map<Integer,Integer> oldToOffset = new HashMap<>();
        int offset = 0;
       for(int i=0;i<newOrders.size();i++){
           oldToNew.put(i,newOrders.get(i));
           oldToOffset.put(i,offset);
           offset=offset+this.inputs.get(newOrders.get(i)).getOutputExpr().size();
       }
       for(int i=0;i<this.outputExpr.size();i++){
           this.outputExpr.set(i,RexNodeHelper.updateTableIndex(this.outputExpr.get(i),oldToNew,oldToOffset));
       }
       Set<RexNode> newConditions = new HashSet<>();
       Iterator<RexNode> it = this.conditions.iterator();
        while(it.hasNext()){
            newConditions.add(RexNodeHelper.updateTableIndex(it.next(),oldToNew,oldToOffset));
        }
        this.conditions = newConditions;
    }


    private Map<Integer, Integer> checkInputMatch(SPJNode node){
        List<AlgeNode> inputs1 = this.inputs;
        List<AlgeNode> inputs2 = node.getInputs();
        if(inputs1.size()==inputs2.size()){
            Map<Integer,Integer> result = new HashMap<>();
            if( checkInputMatch(inputs1,0,inputs2,new HashSet<>(),result)) {
                return result;
            }
        }
        return null;
    }
    private boolean checkInputMatch(List<AlgeNode> inputs1,int index,List<AlgeNode> inputs2,Set<Integer> used,Map<Integer,Integer> inputMatches){
        if(index<inputs1.size()){
            AlgeNode node1 = inputs1.get(index);
            for(int i=0;i<inputs2.size();i++){
                if((!used.contains(i))&&node1.isCartesianEq(inputs2.get(i))){
                    node1.constructSymbolicOutput();
                    inputs2.get(i).constructSymbolicOutput();
                    inputMatches.put(index,i);
                    used.add(i);
                    return checkInputMatch(inputs1,index+1,inputs2,used,inputMatches);
                }
            }
            return false;
        }
        return true;
    }
}
