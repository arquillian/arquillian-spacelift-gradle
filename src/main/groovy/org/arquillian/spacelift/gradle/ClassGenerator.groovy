package org.arquillian.spacelift.gradle

import org.arquillian.spacelift.execution.Task
import org.arquillian.spacelift.process.CommandBuilder
import org.arquillian.spacelift.process.ProcessInteraction

class ClassGenerator {

    static Class<? extends Task<?,?>> createCommandToolClass(Object parentObject, String name, Closure allowedExitCodes, Closure workingDirectory, Closure interaction, Closure isDaemon, Closure environment, Closure command) {

        // create very simple class that extends CommandTool
        // we'll create constructor of the class later on
        String commandClassDefinition = """
            import org.arquillian.spacelift.process.impl.CommandTool;
            import groovy.transform.CompileStatic;

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
            delegate.allowedExitCodes = DSLUtil.resolve(List.class, allowedExitCodes, parentObject)
            delegate.workingDirectory = DSLUtil.resolve(File.class, workingDirectory, parentObject)
            delegate.interaction = DSLUtil.resolve(ProcessInteraction.class, interaction, parentObject)
            delegate.isDaemon = DSLUtil.resolve(Boolean.class, isDaemon, parentObject)
            delegate.environment = DSLUtil.resolve(Map.class, environment, parentObject)
                    // transform to Map<String,String>
                    .collectEntries(new HashMap<String, String>()) {  key, value ->
                        [ (key.toString()):value.toString()]
                    }

            Object commandBuilder = DSLUtil.resolve(command, parentObject)

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
}

