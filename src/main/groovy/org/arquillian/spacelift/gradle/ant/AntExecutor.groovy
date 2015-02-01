package org.arquillian.spacelift.gradle.ant

import org.arquillian.spacelift.Spacelift
import org.arquillian.spacelift.gradle.GradleSpaceliftDelegate
import org.arquillian.spacelift.process.ProcessResult
import org.arquillian.spacelift.task.Task

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

        def command = Spacelift.task('ant')

        if (buildFile) {
            command.parameters('-buildfile', buildFile)
        }

        if (projectDir) {
            command.parameters('-find', projectDir)
        } else {
            throw new IllegalStateException("Can not find an Ant project to build: " + projectDir)
        }

        command.parameters(targets)

        return command.interaction(GradleSpaceliftDelegate.ECHO_OUTPUT).execute().await()
    }
}
