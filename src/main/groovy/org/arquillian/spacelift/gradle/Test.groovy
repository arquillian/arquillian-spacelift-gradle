package org.arquillian.spacelift.gradle

import org.slf4j.Logger

/**
 * Test a
 * @author kpiwko
 *
 */
interface Test extends ContainerizableObject<Test> {

    /**
     * Executes this tests using the logger to notify user about progress
     * @param logger
     * @return
     */
    void executeTest(Logger logger);
}