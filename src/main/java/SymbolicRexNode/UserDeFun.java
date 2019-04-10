package SymbolicRexNode;

import Z3Helper.z3Utility;
import com.microsoft.z3.*;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserDeFun extends RexNodeBase {
    //TODO: this class needs to re-write
    static private Map<String,Map<String,FuncDecl>> registerFunction = new HashMap<>();
    static private Map<String,Map<String,FuncDecl>> registerNullFunction = new HashMap<>();
    static public void reset(){
        // for variable list
        registerFunction = new HashMap<>();
        // for null list
        registerNullFunction = new HashMap<>();
    }
    public UserDeFun(List<SymbolicColumn> inputs, RexNode node, Context z3Context){
        super(inputs,node,z3Context);
        buildExpr(node);
    }
    //TODO: handle function has the same name but different parameters
    private void buildExpr(RexNode node){
        RexCall functionNode = (RexCall) node;
        FuncDecl funcDecl = getFunDecl(functionNode);
        FuncDecl nullFunDecl = getNullFunDecl(functionNode);
        List<RexNode> parameters = functionNode.getOperands();
        Expr[] symbolicParameters = new Expr[parameters.size()*2];
        BoolExpr[] dumpEquivalent = new BoolExpr[parameters.size()];
        for(int i=0;i<parameters.size();i++){
            RexNode parameter = parameters.get(i);
            RexNodeBase converter = RexNodeConverter.getRexConstrains(inputs,parameter,z3Context);
            Expr value = converter.getOutputValue();
            BoolExpr nullValue = converter.getOutputNull();
            Expr dumpVariable = z3Utility.mkVariable(value.getSort(),z3Context);
            BoolExpr nullBranch = z3Context.mkAnd(nullValue,z3Utility.setZero(dumpVariable,z3Context));
            BoolExpr notNullBranch = z3Context.mkAnd(z3Context.mkNot(nullValue),z3Context.mkEq(dumpVariable,value));
            dumpEquivalent[i] = z3Context.mkOr(nullBranch,notNullBranch);
            symbolicParameters[i*2] = dumpVariable;
            symbolicParameters[i*2+1] = nullValue;
            this.assignConstraints.addAll(converter.getAssignConstrains());
        }
        Expr value= z3Context.mkApp(funcDecl,symbolicParameters);
        BoolExpr outputNull= (BoolExpr) z3Context.mkApp(nullFunDecl,symbolicParameters);
        this.output = new SymbolicColumn(value,outputNull,z3Context);
        this.assignConstraints.add(z3Context.mkAnd(dumpEquivalent));
    }
    private FuncDecl getNullFunDecl(RexCall functionNode){
        Sort[] parameterSorts = getParameterSort(functionNode);
        String parameterSignature = getParameterSignature(parameterSorts);
        String name= functionNode.getOperator().getName()+"#null";
        if(registerNullFunction.containsKey(name)){
            Map<String,FuncDecl> theFunction = registerNullFunction.get(name);
            if(theFunction.containsKey(parameterSignature)){
                return theFunction.get(parameterSignature);
            }
        }
        return buildNUllFuncDecl(name,parameterSignature,parameterSorts);
    }
    private FuncDecl getFunDecl(RexCall functionNode){
        Sort[] parameterSorts = getParameterSort(functionNode);
        String parameterSignature = getParameterSignature(parameterSorts);
        String name = functionNode.getOperator().getName();
        if(registerFunction.containsKey(name)){
            Map<String,FuncDecl> theFunction = registerFunction.get(name);
            if(theFunction.containsKey(parameterSignature)){
                return theFunction.get(parameterSignature);
            }
        }
        return buildFuncDecl(functionNode,parameterSignature,parameterSorts);
    }
    private FuncDecl buildNUllFuncDecl(String name, String parameterSignature, Sort[] parameterSorts){
        Map<String,FuncDecl> theFunction = new HashMap<>();
        if (registerNullFunction.containsKey(name)){
            theFunction = registerNullFunction.remove(name);
        }
        String newName = name+"#"+theFunction.size();
        FuncDecl newFuncDecl = z3Context.mkFuncDecl(newName,parameterSorts,z3Context.mkBoolSort());
        theFunction.put(parameterSignature,newFuncDecl);
        registerNullFunction.put(name,theFunction);
        return newFuncDecl;
    }
    private FuncDecl buildFuncDecl(RexCall functionNode, String parameterSignature , Sort[] parameterSorts){
        Map<String,FuncDecl> theFunction = new HashMap<>();
        String name = functionNode.getOperator().getName();
        if(registerFunction.containsKey(name)){
            theFunction = registerFunction.remove(name);
        }
        String newName = name+"#"+theFunction.size();
        Sort returnSort= RexNodeUtility.covertRexNodeSort(z3Context,functionNode);
        FuncDecl newFuncDecl = z3Context.mkFuncDecl(newName,parameterSorts,returnSort);
        theFunction.put(parameterSignature,newFuncDecl);
        registerFunction.put(name,theFunction);
        return newFuncDecl;
    }
    private Sort[] getParameterSort(RexCall functionNode){
        List<RexNode> operands = functionNode.getOperands();
        Sort[] parameterSorts = new Sort[operands.size()*2];
        for (int i = 0;i<operands.size();i++){
            RexNode operand = operands.get(i);
            parameterSorts[2*i] = RexNodeUtility.covertRexNodeSort(z3Context,operand);
            parameterSorts[2*i+1] = z3Context.getBoolSort();
        }
        return parameterSorts;
    }
    private String getParameterSignature(Sort[] parameterSort){
        String result = "";
        for(int i =0;i<parameterSort.length;i++){
            Sort parameter = parameterSort[i];
            if(parameter instanceof BoolSort){
                result = result+"#0";
            }else if(parameter instanceof IntSort){
                result = result +"#1";
            }else if(parameter instanceof RealSort){
                result = result +"#2";
            }
        }
        return result;
    }
}
