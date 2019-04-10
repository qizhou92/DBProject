package AlgeNodeParser;

import AlgeNode.AlgeNode;
import RexNodeHelper.RexNodeHelper;
import com.microsoft.z3.Context;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.logical.LogicalProject;
import org.apache.calcite.rex.RexNode;

import java.util.ArrayList;
import java.util.List;

public class ProjectParser extends AlgeNodeParser{
    public AlgeNode constructRelNode(RelNode input, Context z3Context){
        LogicalProject project = (LogicalProject) input;
        AlgeNode inputNode = AlgeNodeParserPair.constructAlgeNode(project.getInput(),z3Context);
        List<RexNode> oldOutputExpr = inputNode.getOutputExpr();
        List<RexNode> newOutputExpr = new ArrayList<>();
        for(RexNode column:project.getProjects()){
            newOutputExpr.add(RexNodeHelper.substitute(column,oldOutputExpr));
        }
        inputNode.setOutputExpr(newOutputExpr);
        return inputNode;
    }
}
