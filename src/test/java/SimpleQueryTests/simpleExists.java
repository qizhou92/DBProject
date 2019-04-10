package SimpleQueryTests;

import AlgeNode.AlgeNode;
import AlgeNodeParser.AlgeNodeParserPair;
import com.microsoft.z3.Context;
import org.apache.calcite.rel.RelNode;

public class simpleExists {
    public static void main(String[] args) throws  Exception{
        simpleParser parser = new simpleParser();
        RelNode newNode = parser.getRelNode("SELECT * FROM EMP WHERE NOT EXISTS (SELECT * FROM DEPT WHERE DEPT.DEPTNO = EMP.DEPTNO)");
        AlgeNode algeExpr = AlgeNodeParserPair.constructAlgeNode(newNode,new Context());
        System.out.println(algeExpr.toString());
        //SimpleQueryTests.simpleParser parser2 = new SimpleQueryTests.simpleParser();
        //parser2.explain("SELECT * FROM EMP");
    }
}
