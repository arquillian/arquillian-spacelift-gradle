package org.arquillian.spacelift.gradle

import org.gradle.api.Task
import org.slf4j.Logger

/**
 * A test execution controlled by Spacelift
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