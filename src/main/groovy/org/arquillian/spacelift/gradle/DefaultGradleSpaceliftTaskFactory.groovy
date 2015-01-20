package org.arquillian.spacelift.gradle

import groovy.lang.Closure;
import groovy.transform.CompileStatic;

import org.arquillian.spacelift.execution.Task
import org.arquillian.spacelift.process.CommandBuilder
import org.arquillian.spacelift.process.ProcessInteraction
import org.arquillian.spacelift.process.impl.CommandTool
import org.arquillian.spacelift.tool.ToolRegistry
import org.eclipse.jdt.core.dom.ThisExpression;
import org.gradle.api.Project

@CompileStatic
class DefaultGradleSpaceliftTaskFactory extends BaseContainerizableObject<DefaultGradleSpaceliftTaskFactory> implements GradleSpaceliftTaskFactory {

    Closure command = {}

    Closure allowedExitCodes = { new ArrayList<Integer> ()}

    Closure workingDirectory = { CommandTool.CURRENT_USER_DIR }

    Closure interaction = { GradleSpacelift.ECHO_OUTPUT }

    Closure isDaemon = { false }

    Closure environment = { new HashMap<String, String>() }

    DefaultGradleSpaceliftTaskFactory(String name, Project project) {
        super(name, project)
    }

    DefaultGradleSpaceliftTaskFactory(String name, DefaultGradleSpaceliftTaskFactory other) {
        super(name, other)

        this.command = (Closure) other.@command.clone()
        this.allowedExitCodes = (Closure) other.@allowedExitCodes.clone()
        this.workingDirectory = (Closure) other.@workingDirectory.clone()
        this.interaction = (Closure) other.@interaction.clone()
        this.isDaemon = (Closure) other.@isDaemon.clone()
        this.environment = (Closure) other.@environment.clone()
    }

    @Override
    public DefaultGradleSpaceliftTaskFactory clone(String name) {
        return new DefaultGradleSpaceliftTaskFactory(name, this)
    }

    @Override
    public void register(ToolRegistry registry) {
        registry.register(ClassGenerator
                .createCommandToolClass(this, name, allowedExitCodes, workingDirectory, interaction, isDaemon, environment, command))
    }

    @Override
    String toString() {
        return "DefaultGradleSpaceliftTaskFactory for task named ${name}"
    }
}
