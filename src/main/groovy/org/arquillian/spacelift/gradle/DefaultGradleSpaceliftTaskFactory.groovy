package org.arquillian.spacelift.gradle

import org.arquillian.spacelift.execution.Task
import org.arquillian.spacelift.process.CommandBuilder
import org.arquillian.spacelift.process.ProcessInteraction
import org.arquillian.spacelift.process.impl.CommandTool
import org.arquillian.spacelift.tool.ToolRegistry
import org.gradle.api.Project

class DefaultGradleSpaceliftTaskFactory extends BaseContainerizableObject<DefaultGradleSpaceliftTaskFactory> implements GradleSpaceliftTaskFactory {

    Closure command = null

    Closure allowedExitCodes = { new ArrayList<Integer> ()}

    Closure workingDirectory = { CommandTool.CURRENT_USER_DIR }

    Closure interaction = { GradleSpacelift.ECHO_OUTPUT }

    Closure isDaemon = { false }

    Closure environment = { new HashMap<String, String>() }

    // this is not yet supported
    Closure task = null

    DefaultGradleSpaceliftTaskFactory(String name, Project project) {
        super(name, project)
    }

    DefaultGradleSpaceliftTaskFactory(String name, DefaultGradleSpaceliftTaskFactory other) {
        super(name, other)

        this.command = other.@command.clone()
    }

    @Override
    public DefaultGradleSpaceliftTaskFactory clone(String name) {
        return new DefaultGradleSpaceliftTaskFactory(name, this)
    }

    private Class<? extends Task<?,?>> createTaskClass() {

        println "creating task"

        // create command tool
        if(command) {

            println "creating task, command specified"

            // create very simple class that extends CommandTool
            // we'll create constructor of the class later on
            String commandClassDefinition = """
            import org.arquillian.spacelift.process.impl.CommandTool;


            class ToolBinary_${name} extends CommandTool {

                ToolBinary_${name}() {
                    super()
                    // this method is dynamically generated via metaClass
                    initialize()
                }

                protected java.util.Collection aliases() {
                    return ["${name}"]
                }

                public String toString() {
                    return "${name}:" + commandBuilder.toString()
                }
            }
            """

            def classLoader = new GroovyClassLoader()
            def clazz = classLoader.parseClass(commandClassDefinition)

            // generate constructor of metaclass
            clazz.metaClass.initialize = {
                ->
                delegate.allowedExitCodes = DSLUtil.resolve(List.class, allowedExitCodes, owner)
                delegate.workingDirectory = DSLUtil.resolve(File.class, workingDirectory, owner)
                delegate.interaction = DSLUtil.resolve(ProcessInteraction.class, interaction, owner)
                delegate.isDaemon = DSLUtil.resolve(Boolean.class, isDaemon, owner)
                delegate.environment = DSLUtil.resolve(Map.class, environment, owner)

                Object commandBuilder = DSLUtil.resolve(command, owner)

                if(commandBuilder instanceof CommandBuilder) {
                    delegate.commandBuilder = commandBuilder
                }
                else if(commandBuilder instanceof CharSequence || commandBuilder instanceof CharSequence[]) {
                    delegate.commandBuilder = new CommandBuilder(commandBuilder)
                }
                else if(commandBuilder instanceof List) {
                    delegate.commandBuilder = new CommandBuilder(commandBuilder as CharSequence[])
                }
                else {
                    throw new IllegalArgumentException("Invalid definition of command in DSL, should be String, String[] or CommandBuilder but was ${commandBuilder}")
                }

                return delegate
            }

            return clazz

        }

        // this is not yet supported
        throw new UnsupportedOperationException("Invalid tool definition, command block was null")
    }

    @Override
    public void register(ToolRegistry registry) {
        registry.register(createTaskClass())
    }

    @Override
    String toString() {
         return "DefaultGradleSpaceliftTaskFactory for task named ${name}"
    }
}
