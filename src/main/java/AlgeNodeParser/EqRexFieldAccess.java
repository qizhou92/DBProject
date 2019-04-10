package AlgeNodeParser;

import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexInputRef;

public class EqRexFieldAccess extends RexInputRef {
     int tableIndex;
     int fieldIndex;
     int offset;
     public EqRexFieldAccess(int index, RelDataType type, int tableIndex){
         super(index,type);
       this.tableIndex = tableIndex;
       this.fieldIndex = index;
     }
     public int getTableIndex(){
         return tableIndex;
     }

     public int getFieldIndex(){
         return fieldIndex;
     }

     public void setOffset(int offset){
         this.offset = offset;
     }

     public int getOffsetIndex(){
         return (offset+fieldIndex);
     }

    @Override
    public String toString() {
        String result = "["+tableIndex+","+fieldIndex+ "]";
        return result;
    }
}
