package AlgeNode;

import java.util.List;

public class UnionNode extends AlgeNode{
    public UnionNode(List<AlgeNode> inputTables){
        this.inputs = inputTables;
    }
    public boolean isCartesianEq(AlgeNode node){
        return false;
    }
}
