package AlgeNodeParser;

import AlgeNode.AlgeNode;
import RexNodeHelper.RexNodeHelper;
import com.microsoft.z3.Context;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.logical.LogicalFilter;
import org.apache.calcite.rex.RexNode;

import java.util.ArrayList;

public class FilterParser extends AlgeNodeParser{
    public AlgeNode constructRelNode(RelNode input, Context z3Context){
        LogicalFilter filter = (LogicalFilter)input;
        AlgeNode inputNode = AlgeNodeParserPair.constructAlgeNode(filter.getInput(),z3Context);
        RexNode newCondition = RexNodeHelper.substitute(filter.getCondition(),inputNode.getOutputExpr());
        inputNode.addCondition(newCondition);
        return inputNode;
    }
    // need to implement
    private ArrayList<RexNode> conjunctiveForm(RexNode condition){
        ArrayList<RexNode> conjunctiveForm = new ArrayList<>();
        conjunctiveForm.add(condition);
        return conjunctiveForm;
    }
}
