package AlgeNode;
import SymbolicRexNode.BoolPredicate;
import SymbolicRexNode.RexNodeBase;
import SymbolicRexNode.RexNodeConverter;
import SymbolicRexNode.SymbolicColumn;
import Z3Helper.z3Utility;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexNode;
import sun.jvm.hotspot.debugger.cdbg.Sym;

import java.util.*;

public class TableNode extends AlgeNode{
    String  name;
    public TableNode(String name, ArrayList<RexNode> outputExpr, List<RelDataType> inputTypes, Context z3Context){
        setBasicFields(z3Context,inputTypes,new ArrayList<>(),outputExpr,new HashSet<>());
        this.name = name;
    }
    @Override
    public String toString() {
        String result = "Table: "+name+"\n";
        return  result+super.toString();
    }
    public String getName(){
        return this.name;
    }
    public boolean isCartesianEq(AlgeNode node) {
        if(node instanceof TableNode) {
            TableNode table2 = (TableNode) node;
            if (this.name.equals(table2.getName())) {
                //System.out.println("inputs are equal2");
                matchSymbolicInputs(table2);
                return checkSymbolicCondition(table2);
            }
        }
        return false;
    }

    public void matchSymbolicInputs(TableNode node){
        List<SymbolicColumn> symbolicTuple = constructSymbolicTuple();
        this.constructSymbolicInputs(symbolicTuple);
        node.constructSymbolicInputs(symbolicTuple);
    }

    public void constructSymbolicInputs(List<SymbolicColumn> symbolicTuple){
        this.inputSymbolicColumns = symbolicTuple;
    }

    private List<SymbolicColumn> constructSymbolicTuple(){
         List<SymbolicColumn> result = new ArrayList<>();
         for(int i=0;i<inputTypes.size();i++){
             result.add(SymbolicColumn.mkNewSymbolicColumn(z3Context,inputTypes.get(i)));
         }
         return result;
    }

}
