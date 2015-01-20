package org.arquillian.spacelift.gradle

import org.arquillian.spacelift.execution.Task
import org.arquillian.spacelift.tool.ToolRegistry


interface GradleSpaceliftTaskFactory extends ContainerizableObject<GradleSpaceliftTaskFactory> {

    void register(ToolRegistry registry);
}
