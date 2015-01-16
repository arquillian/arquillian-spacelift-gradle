package org.arquillian.spacelift.gradle

import groovy.lang.Closure;

import java.util.Map;

import org.gradle.api.Project
import org.slf4j.Logger

class Test extends BaseContainerizableObject<Test> implements ContainerizableObject<Test> {

    Closure execute = {}

    Closure dataProvider = {[null]}

    Closure beforeSuite = {}

    Closure beforeTest = {}

    Closure afterSuite = {}

    Closure afterTest = {}

    Test(String testName, Project project) {
        super(testName, project)
    }

    /**
     * Cloning constructor. Preserves lazy nature of closures to be evaluated later on.
     * @param other Test to be cloned
     */
    Test(String testName, Test other) {
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
    public Test clone(String name) {
        return new Test(name, this);
    }


    def executeTest(Logger logger) {

        // before suite
        logger.info(":test:${name} before suite execution")
        beforeSuite.rehydrate(new GradleSpaceliftDelegate(), this, this).call()

        // in case anything in this try block fails, we will still run the `after suite` in the finally block
        try {
            // iterate through beforeTest, execute and afterTest based on data provider
            dataProvider.rehydrate(new GradleSpaceliftDelegate(), this, this).call().each { data ->

                if (data == null) {
                    logger.info(":test:${name} before test execution")
                    beforeTest.rehydrate(new GradleSpaceliftDelegate(), this, this).call()
                } else {
                    logger.info(":test:${name} before test execution (${data})")
                    beforeTest.rehydrate(new GradleSpaceliftDelegate(), this, this).call(data)
                }

                // in case anything in this try block fails, we will still run the `after test` in the finally block
                try {
                    if (data == null) {
                        logger.lifecycle(":test:${name}")
                        execute.rehydrate(new GradleSpaceliftDelegate(), this, this).call()
                    } else {
                        logger.lifecycle(":test:${name} (${data})")
                        execute.rehydrate(new GradleSpaceliftDelegate(), this, this).call(data)
                    }
                } finally {
                    if (data == null) {
                        logger.info(":test:${name} after test execution")
                        afterTest.rehydrate(new GradleSpaceliftDelegate(), this, this).call()
                    } else {
                        logger.info(":test:${name} after test execution (${data})")
                        afterTest.rehydrate(new GradleSpaceliftDelegate(), this, this).call(data)
                    }
                }
            }
        } finally {
            // after suite
            logger.info(":test:${name} after suite execution")
            afterSuite.rehydrate(new GradleSpaceliftDelegate(), this, this).call()
        }
    }
}