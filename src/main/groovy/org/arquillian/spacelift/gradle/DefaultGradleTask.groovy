package org.arquillian.spacelift.gradle

import groovy.transform.CompileStatic

import org.arquillian.spacelift.Spacelift
import org.arquillian.spacelift.process.CommandBuilder
import org.arquillian.spacelift.process.ProcessInteraction
import org.arquillian.spacelift.task.Task
import org.arquillian.spacelift.task.TaskFactory
import org.arquillian.spacelift.task.os.CommandTool

@CompileStatic
class DefaultGradleTask extends BaseContainerizableObject<DefaultGradleTask> implements GradleTask {

    DeferredValue<Object> command = DeferredValue.of(Object.class)

    DeferredValue<List> allowedExitCodes = DeferredValue.of(List.class).from([])

    DeferredValue<File> workingDirectory = DeferredValue.of(File.class).from(CommandTool.CURRENT_USER_DIR)

    DeferredValue<ProcessInteraction> interaction = DeferredValue.of(ProcessInteraction.class).from( GradleSpaceliftDelegate.ECHO_OUTPUT )

    DeferredValue<Boolean> isDaemon = DeferredValue.of(Boolean.class).from(false)

    DeferredValue<Map> environment = DeferredValue.of(Map.class).from([:])

    DefaultGradleTask(String name, Object parent) {
        super(name, parent)
    }

    DefaultGradleTask(String name, DefaultGradleTask other) {
        super(name, other)

        this.command = other.@command.copy()
        this.allowedExitCodes = other.@allowedExitCodes.copy()
        this.workingDirectory = other.@workingDirectory.copy()
        this.interaction = other.@interaction.copy()
        this.isDaemon = other.@isDaemon.copy()
        this.environment = other.@environment.copy()
    }

    @Override
    public DefaultGradleTask clone(String name) {
        return new DefaultGradleTask(name, this)
    }

    @Override
    public <IN, OUT, TASK extends Task<? super IN, OUT>> TaskFactory<IN, OUT, TASK> factory() {

        final DefaultGradleTask parent = this

        return new TaskFactory() {
                    Task create() {
                        Task task = (Task) new CommandTool() {
                                    {
                                        this.allowedExitCodes = allowedExitCodes.resolveWith(parent)
                                        this.workingDirectory = workingDirectory.resolveWith(parent)
                                        this.interaction = interaction.resolveWith(parent)
                                        this.isDaemon = isDaemon.resolveWith(parent)
                                        // FIXME transform to Map<String,String>, this should be handled by Spacelift
                                        this.environment = environment.resolveWith(parent)
                                        .collectEntries(new HashMap<String, String>()) {  key, value ->
                                            [ (key.toString()):value.toString()]
                                        }


                                        Object commandBuilder = command.resolveWith(parent)

                                        if(commandBuilder instanceof CommandBuilder) {
                                            this.commandBuilder = (CommandBuilder) commandBuilder
                                        }
                                        else if(commandBuilder instanceof CharSequence || commandBuilder instanceof CharSequence[]) {
                                            this.commandBuilder = new CommandBuilder(commandBuilder)
                                        }
                                        else if(commandBuilder instanceof List) {
                                            this.commandBuilder = new CommandBuilder(commandBuilder as CharSequence[])
                                        }
                                        else {
                                            throw new IllegalArgumentException("Invalid definition of command in DSL, should be String, String[] or CommandBuilder but was ${commandBuilder}")
                                        }
                                    }

                                    @Override
                                    public String toString() {
                                        return "${name}:" + commandBuilder.toString()
                                    }
                                }
                        // FIXME, injector approach does to work due to different package
                        // Nasty, setting private fields
                        task.executionService = Spacelift.service();
                        return task
                    }

                    Collection aliases() {
                        return [name]
                    };
                }
    }

    @Override
    public String toString() {
        return "GradleTask ${name}"
    }
}
