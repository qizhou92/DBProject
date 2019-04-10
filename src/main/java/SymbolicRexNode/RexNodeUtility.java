package SymbolicRexNode;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Sort;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.sql.type.SqlTypeName;

public class RexNodeUtility {
    static public Sort covertRexNodeSort(Context z3Context, RexNode node){
        SqlTypeName type = node.getType().getSqlTypeName();
        return getSortBasedOnSqlType(z3Context,type);
    }
    static public Sort getSortBasedOnSqlType(Context z3Context, SqlTypeName type){
        if(SqlTypeName.APPROX_TYPES.contains(type)){
            return z3Context.mkRealSort();
        }
        if(SqlTypeName.INT_TYPES.contains(type)){
            return z3Context.mkIntSort();
        }
        if(type.equals(SqlTypeName.DECIMAL)){
            return z3Context.mkRealSort();
        }
        if(SqlTypeName.BOOLEAN_TYPES.contains(type)){
            return z3Context.mkBoolSort();
        }
        if(type.equals(SqlTypeName.CHAR)) {
            return z3Context.mkIntSort();
        }
        if(type.equals(SqlTypeName.VARCHAR)){
            return z3Context.mkIntSort();
        }
        return z3Context.mkIntSort();
    }
}
