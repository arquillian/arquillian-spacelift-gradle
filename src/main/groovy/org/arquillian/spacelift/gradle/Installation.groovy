package org.arquillian.spacelift.gradle

import org.arquillian.spacelift.execution.Tasks
import org.arquillian.spacelift.tool.basic.DownloadTool
import org.arquillian.spacelift.tool.basic.UntarTool
import org.arquillian.spacelift.tool.basic.UnzipTool
import org.gradle.api.Project
import org.slf4j.Logger


/**
 * This class defines anything installable
 */
interface Installation extends ContainerizableObject<Installation> {

    /**
     * Returns product identified of the installation
     * @return
     */
    String getProduct()

    /**
     * Returns version of the product
     * @return
     */
    String getVersion()

    /**
     * Returns directory where installation is installed
     * @return
     */
    File getHome()

    /**
     * Checks whether the installation was already installed
     * @return @{code true} if installation is already installed, @{code false} otherwise
     */
    boolean isInstalled()

    /**
     * Installs installation into Spacelift workspace
     * @param logger
     */
    void install(Logger logger)
}