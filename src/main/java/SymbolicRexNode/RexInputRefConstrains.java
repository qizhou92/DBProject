package SymbolicRexNode;

import AlgeNodeParser.EqRexFieldAccess;
import com.microsoft.z3.Context;
import org.apache.calcite.rex.RexNode;

import java.util.List;

public class RexInputRefConstrains extends RexNodeBase {
    public RexInputRefConstrains(List<SymbolicColumn> inputs, RexNode node, Context z3Context){
        super(inputs,node,z3Context);
        buildExpr(node);

    }
    private void buildExpr(RexNode node){
        EqRexFieldAccess inputRef = (EqRexFieldAccess) node;
        int index = inputRef.getOffsetIndex();
        this.output = inputs.get(index);
    }
}
