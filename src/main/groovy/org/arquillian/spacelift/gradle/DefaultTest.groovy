package org.arquillian.spacelift.gradle

import groovy.lang.Closure;

import org.gradle.api.Project;
import org.slf4j.Logger;

class DefaultTest extends BaseContainerizableObject<DefaultTest> implements Test {

    Closure execute = {}

    Closure dataProvider = { [null]}

    Closure beforeSuite = {}

    Closure beforeTest = {}

    Closure afterSuite = {}

    Closure afterTest = {}

    DefaultTest(String testName, Project project) {
        super(testName, project)
    }

    /**
     * Cloning constructor. Preserves lazy nature of closures to be evaluated later on.
     * @param other Test to be cloned
     */
    DefaultTest(String testName, Test other) {
        super(testName, other)
        // use direct access to skip call of getter
        this.execute = other.@execute.clone()
        this.dataProvider = other.@dataProvider.clone()
        this.beforeSuite = other.@beforeSuite.clone()
        this.beforeTest = other.@beforeTest.clone()
        this.afterSuite = other.@afterSuite.clone()
        this.afterTest = other.@afterTest.clone()
    }

    @Override
    public DefaultTest clone(String name) {
        return new DefaultTest(name, this);
    }

    @Override
    void executeTest(Logger logger) {

        // before suite
        logger.info(":test:${name} before suite execution")
        DSLUtil.resolve(beforeSuite, this)

        // in case anything in this try block fails, we will still run the `after suite` in the finally block
        try {
            // iterate through beforeTest, execute and afterTest based on data provider
            DSLUtil.resolve(List.class, dataProvider, this).each { data ->

                if (data == null) {
                    logger.info(":test:${name} before test execution")
                    DSLUtil.resolve(beforeTest, this)
                } else {
                    logger.info(":test:${name} before test execution (${data})")
                    DSLUtil.resolve(Object.class, beforeTest, new GradleSpaceliftDelegate(), this, this, data)
                }

                // in case anything in this try block fails, we will still run the `after test` in the finally block
                try {
                    if (data == null) {
                        logger.lifecycle(":test:${name}")
                        DSLUtil.resolve(execute, this)
                    } else {
                        logger.lifecycle(":test:${name} (${data})")
                        DSLUtil.resolve(Object.class, execute, new GradleSpaceliftDelegate(), this, this, data)
                    }
                } finally {
                    if (data == null) {
                        logger.info(":test:${name} after test execution")
                        DSLUtil.resolve(afterTest, this)
                    } else {
                        logger.info(":test:${name} after test execution (${data})")
                        DSLUtil.resolve(Object.class, afterTest, new GradleSpaceliftDelegate(), this, this, data)
                    }
                }
            }
        } finally {
            // after suite
            logger.info(":test:${name} after suite execution")
            DSLUtil.resolve(afterSuite, this)
        }
    }
}
