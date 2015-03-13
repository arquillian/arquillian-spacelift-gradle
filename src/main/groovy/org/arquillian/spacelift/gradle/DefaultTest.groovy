package org.arquillian.spacelift.gradle

import groovy.transform.CompileStatic

import org.slf4j.Logger

@CompileStatic
class DefaultTest extends BaseContainerizableObject<DefaultTest> implements Test {

    DeferredValue<Void> execute = DeferredValue.of(Void.class)

    DeferredValue<List> dataProvider = DeferredValue.of(List.class).from([null])

    DeferredValue<Void> beforeSuite = DeferredValue.of(Void.class)

    DeferredValue<Void> beforeTest = DeferredValue.of(Void.class)

    DeferredValue<Void> afterSuite = DeferredValue.of(Void.class)

    DeferredValue<Void> afterTest = DeferredValue.of(Void.class)

    DefaultTest(String testName, Object parent) {
        super(testName, parent)
    }

    /**
     * Cloning constructor. Preserves lazy nature of closures to be evaluated later on.
     * @param other Test to be cloned
     */
    DefaultTest(String testName, DefaultTest other) {
        super(testName, other)
        // use direct access to skip call of getter
        this.execute = other.@execute.copy()
        this.dataProvider = other.@dataProvider.copy()
        this.beforeSuite = other.@beforeSuite.copy()
        this.beforeTest = other.@beforeTest.copy()
        this.afterSuite = other.@afterSuite.copy()
        this.afterTest = other.@afterTest.copy()
    }

    @Override
    public DefaultTest clone(String name) {
        return new DefaultTest(name, this);
    }

    @Override
    void executeTest(Logger logger) {

        // before suite
        try {
            logger.info(":test:${name} before suite execution")
            beforeSuite.resolve()
        }
        catch(Exception e) {
            logger.error(":test:${name} failed before suite phase: ${e.getMessage()}")
            throw e
        }

        Exception cause = null

        // in case anything in this try block fails, we will still run the `after suite` in the finally block
        try {
            // iterate through beforeTest, execute and afterTest based on data provider
            dataProvider.resolve().each { data ->

                String dataString =  data ? " (${data})" : ""

                try {
                    logger.info(":test:${name} before test execution${dataString}")
                    beforeTest.resolveWith(this, data)
                }
                catch(Exception e) {
                    logger.error(":test:${name} failed before test phase: ${e.getMessage()}")
                    throw e
                }

                // in case anything in this try block fails, we will still run the `after test` in the finally block
                try {
                    logger.invokeMethod("lifecycle", ":test:${name}${dataString}")
                    execute.resolveWith(this, data)
                }
                catch(Exception e) {
                    logger.error(":test:${name} failed execute phase: ${e.getMessage()}")
                    cause = e
                }
                // clean up
                finally {
                    try {
                        logger.info(":test:${name} after test execution${dataString}")
                        afterTest.resolveWith(this, data)
                    }
                    catch(Exception e) {
                        logger.error(":test:${name} failed after test phase: ${e.getMessage()}")
                        if(cause==null) {
                            cause = e
                        }
                    }
                    if(cause) {
                        throw cause
                    }
                }
            }
        } finally {
            // after suite
            try {
                logger.info(":test:${name} after suite execution")
                afterSuite.resolve()
            }
            catch(Exception e) {
                logger.error(":test:${name} failed after suite phase: ${e.getMessage()}")
                if(cause==null) {
                    cause = e
                }
            }
            finally {
                if(cause) {
                    throw cause
                }
            }
        }
    }
}
