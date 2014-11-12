package org.arquillian.spacelift.gradle

import org.apache.commons.lang3.SystemUtils
import org.apache.log4j.Logger
import org.arquillian.spacelift.process.CommandBuilder
import org.arquillian.spacelift.process.impl.CommandTool
import org.arquillian.spacelift.execution.Tasks
import org.arquillian.spacelift.tool.ToolRegistry
import org.gradle.api.Project

/**
 * A tool that can be dynamically registered in Spacelift in Gradle build
 * @author <a href="kpiwko@redhat.com">Karel Piwko</a>
 *
 */
@Mixin(ValueExtractor)
class GradleSpaceliftTool {
    private static final Logger logger = Logger.getLogger("Spacelift")

    // required by gradle to be defined
    String name

    // prepared command tool
    Closure command

    Project project

    GradleSpaceliftTool(String toolName, Project project) {
        this.name = toolName
        this.project = project
    }

    GradleSpaceliftTool(Project project) {
        this.project = project
    }


    void registerInSpacelift(ToolRegistry registry) {

        // dynamically construct class that represent the tool
        def toolClass = """
            import org.arquillian.spacelift.process.CommandBuilder
            import org.arquillian.spacelift.process.impl.CommandTool
            import java.util.Map;

            class ToolBinary_${name} extends CommandTool {

                static CommandTool commandTool

                ToolBinary_${name}() {
                    if (commandTool != null) {
                        // clone environment
                        super.environment.putAll(commandTool.environment)
                        // set command builder, clone it to get a fresh instance
                        super.commandBuilder = new CommandBuilder(commandTool.commandBuilder);
                    }
                }

                protected java.util.Collection aliases() {
                    return ["${name}"]
                }

                public String toString() {
                    return new StringBuilder("${name}: ").append(super.commandBuilder.toString()).toString()
                }
            }
        """

        def classLoader = new GroovyClassLoader()
        def clazz = classLoader.parseClass(toolClass)
        def instance = clazz.newInstance()

        // FIXME here we access static fields via instance as we don't have class object
        instance.commandTool = getCommand()

        // register tool using dynamically constructed class
        registry.register(clazz)

        logger.info("Tool ${name} was registered")
    }

    def command(arg) {
        this.command = extractValueAsLazyClosure(arg).dehydrate()
        this.command.resolveStrategy = Closure.DELEGATE_FIRST
    }

    def getCommand() {
        def commandTool = command.rehydrate(new GradleSpaceliftDelegate(), this, this).call()
        if(commandTool==null) {
            return Tasks.prepare(CommandTool)
        }

        if (commandTool instanceof CommandTool) {
            return commandTool
        }
        else if(commandTool instanceof Collection) {
            CommandBuilder theCommand = new CommandBuilder(commandTool[0].toString())
            commandTool.eachWithIndex { param, i ->
                if(i!=0) {
                    theCommand.parameters(param.toString())
                }
            }
            return Tasks.prepare(CommandTool).command(theCommand)
        }

        return Tasks.prepare(CommandTool).command(new CommandBuilder(commandTool.toString()));
    }

    @Override
    public String toString() {
        return "SpaceliftTool ${name}, commandTool: ${getCommand()}"
    }
}
