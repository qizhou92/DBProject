package AlgeNodeParser;

import AlgeNode.AlgeNode;
import AlgeNode.TableNode;
import com.microsoft.z3.Context;
import org.apache.calcite.adapter.enumerable.EnumerableTableScan;
import org.apache.calcite.plan.RelOptTable;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.rex.RexNode;

import java.util.ArrayList;
import java.util.List;


public class TableParser extends AlgeNodeParser{
    public AlgeNode constructRelNode(RelNode input, Context z3Context){
        EnumerableTableScan tableScan = (EnumerableTableScan) input;
        RelOptTable table = tableScan.getTable();
        String tableName = table.getQualifiedName().get(0);
        List<RelDataTypeField> columns = tableScan.getRowType().getFieldList();
        ArrayList<RexNode> outputExpr = new ArrayList<>();
        ArrayList<RelDataType> columnTypes = new ArrayList<>();
        int count = 0;
        for (RelDataTypeField column:columns){
            columnTypes.add(column.getType());
            EqRexFieldAccess expr = new EqRexFieldAccess(count,column.getType(),0);
            outputExpr.add(expr);
            count++;
        }
        TableNode tableNode = new TableNode(tableName,outputExpr,columnTypes,z3Context);
        return tableNode;
    }
}
