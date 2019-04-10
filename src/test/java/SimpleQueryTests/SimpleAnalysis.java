package SimpleQueryTests;

import AlgeNode.AlgeNode;
import AlgeNodeParser.AlgeNodeParserPair;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.z3.Context;
import org.apache.calcite.rel.RelNode;

import java.io.*;

public class SimpleAnalysis {
    public static boolean BeVerified(String sql1, String sql2, String name, PrintWriter cannotCompile, PrintWriter cannotProve, PrintWriter prove){
        RelNode logicPlan = null;
        RelNode logicPlan2 = null;
        if((!notContains(sql1))|| (!notContains(sql2))) {
            return false;
        }
        try {
            simpleParser parser = new simpleParser();
            logicPlan = parser.getRelNode(sql1);
            simpleParser parser2 = new simpleParser();
            logicPlan2 = parser2.getRelNode(sql2);
            Context z3Context = new Context();
            AlgeNode algeExpr = AlgeNodeParserPair.constructAlgeNode(logicPlan,z3Context);
            AlgeNode algeExpr2 = AlgeNodeParserPair.constructAlgeNode(logicPlan2,z3Context);
            //System.out.println(algeExpr);
            //System.out.println(algeExpr2);
            try {
                if(algeExpr.isEq(algeExpr2)){
                    System.out.println("it proves");
                    prove.println(name);
                    prove.flush();
                }else{
                    System.out.println("it fails");
                    cannotProve.println(name);
                    cannotProve.flush();
                }
            }catch (Exception e){
                System.out.println(name);
            }
            //System.out.println("it ends here");
            z3Context.close();
            //System.out.println(name);
            return true;
        }catch (Exception e) {
            System.out.println();
            return false;
        }
    }
    public static boolean simpleVerify(String sql1, String sql2){
        RelNode logicPlan = null;
        RelNode logicPlan2 = null;
        try {
            simpleParser parser = new simpleParser();
            logicPlan = parser.getRelNode(sql1);
            simpleParser parser2 = new simpleParser();
            logicPlan2 = parser2.getRelNode(sql2);
        }catch (Exception e){
            System.out.println(e);
        }
        Context z3Context = new Context();
        AlgeNode algeExpr = AlgeNodeParserPair.constructAlgeNode(logicPlan,z3Context);
        AlgeNode algeExpr2 = AlgeNodeParserPair.constructAlgeNode(logicPlan2,z3Context);
        System.out.println(algeExpr);
        System.out.println(algeExpr2);
        boolean result = algeExpr.isEq(algeExpr2);
        return result;
    }
    public static void main(String[] args) throws Exception {
        File f = new File("testData/calcite_tests.json");
        JsonParser parser = new JsonParser();
        JsonArray array = parser.parse(new FileReader(f)).getAsJsonArray();
        FileWriter prove = new FileWriter("calciteProve.txt");
        BufferedWriter bw = new BufferedWriter(prove);
        PrintWriter out = new PrintWriter(bw);
        FileWriter notProve = new FileWriter("calciteNotProve.txt");
        BufferedWriter bw2 = new BufferedWriter(notProve);
        PrintWriter out2 = new PrintWriter(bw2);
        FileWriter notCompile = new FileWriter("cannotCompile.txt");
        BufferedWriter bw3 = new BufferedWriter(notCompile);
        PrintWriter out3 = new PrintWriter(bw3);
        int count = 0;
        long time = 0;
        for(int i=0;i<array.size();i++){
            JsonObject testCase = array.get(i).getAsJsonObject();
            String query1 = testCase.get("q1").getAsString();
            String query2 = testCase.get("q2").getAsString();
            String name = testCase.get("name").getAsString();

            /**
            if(!name.equals("testDistinctCountGroupingSets1")){
                continue;
            }**/

            try{
                boolean result = BeVerified(query1,query2,name,out3,out2,out);
                //boolean result = simpleVerify(query1,query2);
                if(result){
                    count++;
                }
            }catch (Exception e){
            }

        }
        System.out.println("what is the number:"+count);
        out.close();
        out2.close();
        out3.close();
//        String query1 = load.get("skeynet_id1")+"_"+load.get("instance_id1");
//        String query2 = load.get("skeynet_id2")+"_"+load.get("instance_id2");
//        String sql = "SELECT 1 FROM EMP AS EMP INNER JOIN EMP AS EMP0 ON EMP.DEPTNO = EMP0.DEPTNO;";
//        String sql2 ="SELECT 1 FROM EMP AS EMP1 INNER JOIN EMP AS EMP2 ON EMP1.DEPTNO = EMP2.DEPTNO;";
//        boolean result = BeVerified(sql,sql2);
//        System.out.println(result);
    }
    static public boolean notContains(String sql){
        String[] keyWords ={"VALUE","EXISTS","ROW","ORDER","CAST","INTERSECT","EXCEPT","LEFT","FULL","RIGHT"," IN ","UNION"};
        for(int i=0;i<keyWords.length;i++){
            if(sql.contains(keyWords[i])){
                return false;
            }
        }
        return true;
    }
}
