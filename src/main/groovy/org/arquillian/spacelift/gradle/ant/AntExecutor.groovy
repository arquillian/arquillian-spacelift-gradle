package org.arquillian.spacelift.gradle.ant

import java.text.MessageFormat
import java.util.Collection;

import org.arquillian.spacelift.execution.Task
import org.arquillian.spacelift.process.Command
import org.arquillian.spacelift.process.CommandBuilder
import org.arquillian.spacelift.process.ProcessInteractionBuilder
import org.arquillian.spacelift.process.ProcessResult;
import org.arquillian.spacelift.gradle.GradleSpacelift;
import org.arquillian.spacelift.gradle.utils.EnvironmentUtils

class AntExecutor extends Task<Object, ProcessResult>{

    def projectDir

    def targets = []

    def buildFile

    AntExecutor target(CharSequence target) {
        targets << target
        this
    }

    AntExecutor targets(CharSequence...targets) {
        this.targets.addAll(targets)
        this
    }

    AntExecutor buildFile(String file) {
        this.buildFile = file
        this
    }

    AntExecutor projectDir(String projectDir) {
        this.projectDir = projectDir
        this
    }

    @Override
    protected ProcessResult process(Object input) throws Exception {

        def command = GradleSpacelift.tools('ant')

        if (buildFile) {
            command.parameters('-buildfile', buildFile)
        }

        if (projectDir) {
            command.parameters('-find', projectDir)
        } else {
            throw new IllegalStateException("Can not find an Ant project to build: " + projectDir)
        }

        command.parameters(targets)

        return command.interaction(GradleSpacelift.ECHO_OUTPUT).execute().await()
    }
}
