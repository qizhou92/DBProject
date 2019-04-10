package Z3Helper;
import SymbolicRexNode.RexNodeUtility;
import SymbolicRexNode.SymbolicColumn;
import com.microsoft.z3.*;
import org.apache.calcite.rel.type.RelDataType;

import java.util.*;


public class z3Utility {
    static private int count = 0;
    static private Map<Expr,Integer> variableId= new HashMap<>();
    static public void reset(){
        count = 0;
        variableId = new HashMap<>();
    }
    static public boolean isConditionEq(List<BoolExpr> assignConstraints,BoolExpr condition1,BoolExpr condition2,Context z3Context){
        BoolExpr[] equation = new BoolExpr[assignConstraints.size()+1];
        for(int i=0;i<assignConstraints.size();i++){
            equation[i]=assignConstraints.get(i);
        }
        equation[assignConstraints.size()]=z3Context.mkNot(z3Context.mkEq(condition1,condition2));
        Solver s = z3Context.mkSolver();
        s.add(equation);
        if(s.check()==Status.UNSATISFIABLE){
            return true;
        }else{
            return false;
        }
    }

    static public boolean symbolicOutputEqual(BoolExpr conditions,List<SymbolicColumn> list1,List<SymbolicColumn> list2, Context z3Context){
        if(list1.size() == list2.size()){
            BoolExpr[] columnEqs = new BoolExpr[list1.size()];
            for(int i=0;i<list1.size();i++){
                columnEqs[i] = symbolicColumnEq(list1.get(i),list2.get(i),z3Context);
            }
            BoolExpr notEq = z3Context.mkNot(z3Context.mkAnd(columnEqs));
            Solver s = z3Context.mkSolver();
            s.add(z3Context.mkAnd(conditions,notEq));
            if(s.check()==Status.UNSATISFIABLE){
                return true;
            }
        }

        return false;
    }

    static public boolean symbolicColumnEqual(BoolExpr conditions,SymbolicColumn column1,SymbolicColumn column2,Context z3Context){
        BoolExpr equation = z3Context.mkAnd(conditions,z3Context.mkNot(symbolicColumnEq(column1,column2,z3Context)));
        Solver s = z3Context.mkSolver();
        s.add(equation);
        if(s.check()==Status.UNSATISFIABLE){
            return true;
        }else{
            return false;
        }
    }
    static public boolean symbolicColumnEqual(SymbolicColumn column1,SymbolicColumn column2,Context z3Context){
        BoolExpr equation = z3Context.mkNot(symbolicColumnEq(column1,column2,z3Context));
        Solver s = z3Context.mkSolver();
        s.add(equation);
        if(s.check()==Status.UNSATISFIABLE){
            return true;
        }else{
            return false;
        }
    }
    static public Expr getDumpVariable(Sort sort,Context z3Context){
        return mkVariable(sort,z3Context);
    }
    static public Expr mkDumpValue(RelDataType type,Context z3Context){
        return z3Context.mkConst("dump",RexNodeUtility.getSortBasedOnSqlType(z3Context,type.getSqlTypeName()));
    }
    static public Expr mkVariable(RelDataType type,Context z3Context){
        String name = "Variable"+count;
        Expr newVariable = z3Context.mkConst(name,RexNodeUtility.getSortBasedOnSqlType(z3Context,type.getSqlTypeName()));
        variableId.put(newVariable,count);
        count++;
        return newVariable;
    }
    static public Expr mkVariable(Sort sort,Context z3Context){
        String name = "Variable"+count;
        Expr newVariable = z3Context.mkConst(name,sort);
        variableId.put(newVariable,count);
        count++;
        return newVariable;
    }
    static public int getVariableId(Expr variable){
        if(!variableId.containsKey(variable)){
            System.out.println("it should not have variables that do not have id");
            return 0;
        }
        else{
            return variableId.get(variable);
        }
    }
    public static BoolExpr mkAnd(List<BoolExpr> constrains,Context z3Context){
        BoolExpr[] andC = new BoolExpr[constrains.size()];
        for(int i = 0;i<constrains.size();i++){
            andC[i] = constrains.get(i);
        }
        BoolExpr simplifiedExpr = (BoolExpr) z3Context.mkAnd(andC).simplify();
        return simplifiedExpr;
    }
    public static BoolExpr mkOr(List<BoolExpr> constrains,Context z3Context){
        BoolExpr[] orC = new BoolExpr[constrains.size()];
        for(int i = 0;i<constrains.size();i++){
            orC[i] = constrains.get(i);
        }
        BoolExpr simplifiedExpr = (BoolExpr) z3Context.mkOr(orC).simplify();
        return simplifiedExpr;
    }
    public static BoolExpr setZero(Expr symbolicValue,Context z3Context){
        if(symbolicValue.isReal()){
            return z3Context.mkEq(symbolicValue,z3Context.mkReal(0));
        }
        if(symbolicValue.isBool()){
            return z3Context.mkEq(symbolicValue,z3Context.mkTrue());
        }
        if(symbolicValue.isInt()){
            return z3Context.mkEq(symbolicValue,z3Context.mkInt(0));
        }
        return null;
    }
    public static BoolExpr allFiledNullFunction(List<Expr> variables,Context z3Context){
        BoolExpr[] constrains = new BoolExpr[variables.size()];
        for (int i=0;i<variables.size();i++){
            constrains[i] = nullFunction(variables.get(i),z3Context);
        }
        return z3Context.mkAnd(constrains);
    }
    public static BoolExpr isNull(Set<Expr> variables,Context z3Context){
        BoolExpr[] orNull = new BoolExpr[variables.size()];
        int count = 0;
        for(Expr variable:variables){
            orNull[count] = setNull(variable,z3Context);
            count++;
        }
        if(variables.size()!=0) {
            return z3Context.mkOr(orNull);
        }else{
            return z3Context.mkFalse();
        }
    }
    public static BoolExpr setNotNull( Expr variable,Context z3Context){
        if(!variableId.containsKey(variable)){
            return z3Context.mkTrue();
        }
        BoolExpr nullResult = (BoolExpr) z3Context.mkConst("Null"+z3Utility.getVariableId(variable),z3Context.mkBoolSort());
        return z3Context.mkNot(nullResult);

    }
    public static BoolExpr setNotNull(Set<Expr> variables,Context z3Context){
        BoolExpr[] allNotNull = new BoolExpr[variables.size()];
        int count = 0;
        for(Expr variable:variables){
            allNotNull[count] = setNotNull(variable,z3Context);
            count++;
        }
        return z3Context.mkAnd(allNotNull);
    }
    public static BoolExpr setNull(Expr variable,Context z3Context){
        if(!variableId.containsKey(variable)){
            return z3Context.mkFalse();
        }
        BoolExpr nullResult = (BoolExpr) z3Context.mkConst("Null"+z3Utility.getVariableId(variable),z3Context.mkBoolSort());
        return nullResult;
    }
    public static BoolExpr nullFunction(Expr variable,Context z3Context){
        if(!variableId.containsKey(variable)){
            return z3Context.mkFalse();
        }
        BoolExpr nullResult = (BoolExpr) z3Context.mkConst("Null"+z3Utility.getVariableId(variable),z3Context.mkBoolSort());
        return nullResult;
    }
    public static boolean hasVariable(Expr variable){
        return variableId.containsKey(variable);
    }

    public static boolean isUnat(BoolExpr expr,Context z3Context){
        Solver s = z3Context.mkSolver();
        s.add(expr);
        if(s.check()==Status.UNSATISFIABLE){
            return true;
        }else{
            return false;
        }
    }

    public static boolean entail(BoolExpr expr1, BoolExpr expr2,Context z3Context){
        Solver s = z3Context.mkSolver();
        BoolExpr constrains = z3Context.mkAnd(expr1,z3Context.mkNot(expr2));
        s.add(constrains);
        if(s.check()==Status.UNSATISFIABLE){
            return true;
        }else{
           return false;
        }
    }

    static public BoolExpr symbolicColumnEq(SymbolicColumn column1,SymbolicColumn column2,Context z3Context){
        BoolExpr bothNull = z3Context.mkAnd(column1.getSymbolicNull(),column2.getSymbolicNull());
        BoolExpr valueEq = z3Context.mkAnd(z3Context.mkEq(column1.getSymbolicNull(),column2.getSymbolicNull()),z3Context.mkEq(column1.getSymbolicValue(),column2.getSymbolicValue()));
        return (BoolExpr) z3Context.mkOr(bothNull,valueEq).simplify();
    }
    //TODO
    public static SymbolicColumn mkDumpSymoblicColumn(){
        return null;
    }

    public static List<Expr> collectAllConstant(Expr e){
        List<Expr> variables = new ArrayList<>();
        if(isVariable(e)){
            variables.add(e);
        }else{
            Expr[] subExpres = e.getArgs();
            for(int i=0;i<subExpres.length;i++){
                variables.addAll(collectAllConstant(subExpres[i]));
            }
        }
        return variables;
    }
    // TODO:
    // a temporary methods, might need work in the future.
    public static Expr constructFreshExpr(Expr e,Context z3Context){
        List<Expr> variables = collectAllConstant(e);
        Expr[] oldVariables = new Expr[variables.size()];
        Expr[] newVariables = new Expr[variables.size()];
        for(int i=0;i<variables.size();i++){
            Expr variable = variables.get(i);
            String name = variable.getSExpr();
            Expr freshVariable = z3Context.mkConst(name+"Fresh",variable.getSort());
            oldVariables[i]=variable;
            newVariables[i]=freshVariable;
        }
        return e.substitute(oldVariables,newVariables);
    }
    public static boolean isVariable(Expr e){
       return e.isConst()&&(!isConstant(e));
    }
    public static boolean isConstant(Expr e){
        if(e.isTrue()){
            return true;
        }
        if(e.isFalse()){
            return true;
        }
        if(e.isIntNum()){
            return true;
        }
        if(e.isRatNum()){
            return true;
        }
        return false;
    }
}
