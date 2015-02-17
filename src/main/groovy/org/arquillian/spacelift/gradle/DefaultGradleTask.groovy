package org.arquillian.spacelift.gradle

import groovy.transform.CompileStatic

import org.arquillian.spacelift.Spacelift
import org.arquillian.spacelift.process.CommandBuilder
import org.arquillian.spacelift.process.ProcessInteraction
import org.arquillian.spacelift.task.Task
import org.arquillian.spacelift.task.TaskFactory
import org.arquillian.spacelift.task.os.CommandTool
import org.gradle.api.Project

@CompileStatic
class DefaultGradleTask extends BaseContainerizableObject<DefaultGradleTask> implements GradleTask {

    Closure command = {}

    Closure allowedExitCodes = {
        new ArrayList<Integer> ()
    }

    Closure workingDirectory = { CommandTool.CURRENT_USER_DIR }

    Closure interaction = { GradleSpaceliftDelegate.ECHO_OUTPUT }

    Closure isDaemon = { false }

    Closure environment = {
        new HashMap<String, String>()
    }

    DefaultGradleTask(String name, Object parent) {
        super(name, parent)
    }

    DefaultGradleTask(String name, DefaultGradleTask other) {
        super(name, other)

        this.command = (Closure) other.@command.clone()
        this.allowedExitCodes = (Closure) other.@allowedExitCodes.clone()
        this.workingDirectory = (Closure) other.@workingDirectory.clone()
        this.interaction = (Closure) other.@interaction.clone()
        this.isDaemon = (Closure) other.@isDaemon.clone()
        this.environment = (Closure) other.@environment.clone()
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
                        Task task = (Task) new CommandTool() { {
                                        this.allowedExitCodes = DSLUtil.resolve(List.class, allowedExitCodes, parent)
                                        this.workingDirectory = DSLUtil.resolve(File.class, workingDirectory, parent)
                                        this.interaction = DSLUtil.resolve(ProcessInteraction.class, interaction, parent)
                                        this.isDaemon = DSLUtil.resolve(Boolean.class, isDaemon, parent)
                                        this.environment = DSLUtil.resolve(Map.class, environment, parent)
                                            // FIXME transform to Map<String,String>, this should be handled by Spacelift
                                            .collectEntries(new HashMap<String, String>()) {  key, value ->
                                                [ (key.toString()):value.toString()]
                                            }

                                        Object commandBuilder = DSLUtil.resolve(command, parent)

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
