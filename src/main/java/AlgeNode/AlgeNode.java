package AlgeNode;

import AlgeNodeParser.EqRexFieldAccess;
import SymbolicRexNode.BoolPredicate;
import SymbolicRexNode.RexNodeBase;
import SymbolicRexNode.RexNodeConverter;
import SymbolicRexNode.SymbolicColumn;
import Z3Helper.z3Utility;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexNode;
import java.util.*;
import java.util.concurrent.locks.Condition;

abstract public class AlgeNode {
    protected Context z3Context;
    protected List<RelDataType> inputTypes;
    protected List<AlgeNode> inputs;
    protected List<RexNode> outputExpr;
    protected Set<RexNode> conditions;

    protected List<SymbolicColumn> inputSymbolicColumns;
    protected List<SymbolicColumn> outputSymbolicColumns;
    protected SymbolicColumn symbolicCondition;

    protected List<BoolExpr> conditionAssign;
    protected List<BoolExpr> outputAssign;
    protected List<BoolExpr> inputTupleConstraints;


    protected void setBasicFields(Context z3Context, List<RelDataType> inputTypes, List<AlgeNode> inputs, List<RexNode> outputExpr, Set<RexNode> conditions){
        this.z3Context = z3Context;
        this.inputTypes = inputTypes;
        this.inputs = inputs;
        this.outputExpr = outputExpr;
        this.conditions = conditions;
        this.outputAssign = new ArrayList<>();
        this.conditionAssign = new ArrayList<>();
    }

    // get basic output types
    public List<RelDataType> getInputTypes(){
        return this.inputTypes;
    }

    // get the algebraic nodes inputs
    public List<AlgeNode> getInputs(){
        return this.inputs;
    }

    // set the output expr
    public void setOutputExpr(List<RexNode> outputExpr) {
        this.outputExpr = outputExpr;
    }

    // return the output expression
    public List<RexNode> getOutputExpr(){
        return this.outputExpr;
    }

    // reset the output expression to unit expression
    public void resetOutputExpr(){
        ArrayList<RexNode> newOutputExpr = new ArrayList<RexNode>();
        for(int i=0;i<inputTypes.size();i++){
            RexNode newColumn = new EqRexFieldAccess(i,inputTypes.get(i),0);
            newOutputExpr.add(newColumn);
        }
        this.outputExpr = newOutputExpr;
    }

    // return the set of filter conditions
    public Set<RexNode> getConditions(){
        return this.conditions;
    }

    // reset the filter conditions as the empty set
    public void resetCondition(){
        this.conditions = new HashSet<>();
    }

    // add one condition into the filter condition
    public void addCondition(RexNode condition) {
        this.conditions.add(condition);
    }

    public List<BoolExpr> getConditionAssign(){
        return this.conditionAssign;
    }

    public List<BoolExpr> getOutputAssign(){
        return this.outputAssign;
    }

    public List<BoolExpr> getInputTupleConstraints(){
        return this.inputTupleConstraints;
    }

    // return the symbolic output tuple
    public List<SymbolicColumn> getSymbolicOutput(){
        if(this.outputSymbolicColumns == null){
           constructSymbolicOutput();
        }
        return this.outputSymbolicColumns;
    }

    // construct the symbolic output tuple based on the symbolic input tuple
    protected void constructSymbolicOutput(){
        //System.out.println("make symbolic output");
        //System.out.println(this.outputExpr);
        this.outputAssign = new ArrayList<>();
        this.outputSymbolicColumns = new ArrayList<>();
        for(int i=0;i<this.outputExpr.size();i++){
            //System.out.println("what is wrong?");
            RexNodeBase converter = RexNodeConverter.getRexConstrains(this.inputSymbolicColumns,this.outputExpr.get(i),z3Context);
            //System.out.println("what is wrong here?");
            this.outputSymbolicColumns.add(converter.getOutput());
            this.outputAssign.addAll(converter.getAssignConstrains());
        }
        //System.out.println("finish make symbolic output");
    }

    // check if two nodes symbolic outputs are equivalent with default match
    public boolean checkSymbolicOutput(AlgeNode node){
        if(this == node){
            return true;
        }
        List<SymbolicColumn> symbolicOutputs1 = this.getSymbolicOutput();
        List<SymbolicColumn> symbolicOutputs2 = node.getSymbolicOutput();
        return z3Utility.symbolicOutputEqual(buildOutputEnv(node),symbolicOutputs1,symbolicOutputs2,z3Context);
    }

    public boolean checkSymbolicOutput(AlgeNode node, Map<Integer,Integer> columnPairs){
        Map<Integer,Integer> simplifyColumnPairs = eliminateMatches(node,columnPairs);
        if(simplifyColumnPairs.isEmpty()){
            return true;
        }

        List<SymbolicColumn> symbolicTuple1 = new ArrayList<>();
        List<SymbolicColumn> symbolicTuple2 = new ArrayList<>();
        List<SymbolicColumn> symbolicOutputs1 = this.getSymbolicOutput();
        List<SymbolicColumn> symbolicOutputs2 = node.getSymbolicOutput();

        for(Map.Entry<Integer,Integer> columnPair:simplifyColumnPairs.entrySet()){
            symbolicTuple1.add(symbolicOutputs1.get(columnPair.getKey()));
            symbolicTuple2.add(symbolicOutputs2.get(columnPair.getValue()));
        }

        return z3Utility.symbolicOutputEqual(buildOutputEnv(node),symbolicTuple1,symbolicTuple2,z3Context);
    }

    private Map<Integer,Integer> eliminateMatches(AlgeNode node,Map<Integer,Integer> columnPairs){
        if(this == node) {
            Map<Integer, Integer> newColumnPairs = new HashMap<>();
            for (Map.Entry<Integer, Integer> columnPair : columnPairs.entrySet()) {
                int key = columnPair.getKey();
                int value = columnPair.getValue();
                if (key != value) {
                    newColumnPairs.put(key, value);
                }
            }
            return newColumnPairs;
        }else{
            return columnPairs;
        }
    }

    private BoolExpr buildOutputEnv(AlgeNode node){
        List<BoolExpr> env = new ArrayList<>();
        // builds environment
        env.addAll(this.getInputTupleConstraints());
        env.addAll(node.getInputTupleConstraints());
        env.addAll(this.getOutputAssign());
        env.addAll(node.getOutputAssign());
        env.addAll(this.getConditionAssign());
        env.addAll(node.getConditionAssign());
        env.add(this.getSymbolicCondition().isValueTrue());
        env.add(node.getSymbolicCondition().isValueTrue());
        return z3Utility.mkAnd(env,z3Context);
    }

    // return the symbolic filter conditions
    public SymbolicColumn getSymbolicCondition(){
        if(this.symbolicCondition == null){
            constructSymbolicCondition();
        }
        return this.symbolicCondition;
    }

    // construct the symbolic conditions based the symbolic inputs
    private void constructSymbolicCondition(){
        this.conditionAssign = new ArrayList<>();
        if(this.inputTupleConstraints == null) {
            setInputTupleConstraints();
        }
        if(this.conditions.size() == 0){
            this.symbolicCondition = new SymbolicColumn(z3Context.mkTrue(),z3Context.mkFalse(),z3Context);
        }else{
            this.symbolicCondition = BoolPredicate.getAndNodeSymoblicColumn(this.conditions,this.inputSymbolicColumns,this.conditionAssign,z3Context);
        }
    }

    // check if two node's symbolic condition are logically equivalent
    public boolean checkSymbolicCondition(AlgeNode node){
        SymbolicColumn condition1 = this.getSymbolicCondition();
        SymbolicColumn condition2 = node.getSymbolicCondition();
        List<BoolExpr> env = new ArrayList<>();
        env.addAll(this.getInputTupleConstraints());
        env.addAll(node.getInputTupleConstraints());
        env.addAll(this.getConditionAssign());
        env.addAll(node.getConditionAssign());
        return z3Utility.isConditionEq(env,condition1.isValueTrue(),condition2.isValueTrue(),z3Context);
    }

    public void setInputTupleConstraints(){
        this.inputTupleConstraints = new ArrayList<>();
        for(AlgeNode child:inputs){
            this.inputTupleConstraints.addAll(child.getOutputAssign());
            this.inputTupleConstraints.addAll(child.getConditionAssign());
            this.inputTupleConstraints.add(child.getSymbolicCondition().isValueTrue());
            this.inputTupleConstraints.addAll(child.getInputTupleConstraints());
        }
    }
    public boolean isEq(AlgeNode node) {
        if(isCartesianEq(node)){
            return checkSymbolicOutput(node);
        }
        return false;
    }

    abstract public boolean isCartesianEq(AlgeNode node);
    @Override
    public String toString() {
        String result = "outputExpr: (";
        for(int i=0;i<outputExpr.size();i++){
            result= result+outputExpr.get(i).toString();
        }
        result = result+")\nConditions : (";
        for(RexNode condition:conditions){
            result= result+"["+condition.toString()+"] ,  ";
        }
        result = result+")\n inputTables:\n";
        for(int i=0;i<inputs.size();i++){
            result=result+inputs.get(i).toString()+"\n";
        }
        return result;
    }
}
