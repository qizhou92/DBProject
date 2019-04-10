package AlgeRule;

import AlgeNode.AlgeNode;

public interface AlgeRule {
    boolean  preCondition(AlgeNode input);

    AlgeNode transformation(AlgeNode input);
}
