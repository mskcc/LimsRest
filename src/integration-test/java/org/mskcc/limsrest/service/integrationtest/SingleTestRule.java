package org.mskcc.limsrest.service.integrationtest;

import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

public class SingleTestRule implements MethodRule {
    private String applyMethod;

    public SingleTestRule(String applyMethod) {
        this.applyMethod = applyMethod;
    }

    @Override
    public Statement apply(final Statement statement, final FrameworkMethod method, final Object target) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                if (applyMethod.equals(method.getName())) {
                    statement.evaluate();
                }
            }
        };
    }
}
