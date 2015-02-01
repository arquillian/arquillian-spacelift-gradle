package org.arquillian.spacelift.gradle.java

import org.arquillian.spacelift.Spacelift
import org.arquillian.spacelift.gradle.GradleSpaceliftDelegate
import org.arquillian.spacelift.task.Task

class JavaExecutor extends Task<Object, Void> {

    def workingDir

    def parameters = []

    def jarFile

    JavaExecutor parameter(CharSequence parameter) {
        parameters << parameter
        this
    }

    JavaExecutor parameters(CharSequence...parameters) {
        this.parameters.addAll(parameters)
        this
    }

    JavaExecutor jarFile(String file) {
        this.jarFile = file
        this
    }

    JavaExecutor workingDir(String dir) {
        this.workingDir = dir
        this
    }

    @Override
    protected Void process(Object input) throws Exception {

        def command = Spacelift.task('java')

        if(workingDir) {
            command.workingDir(workingDir)
        }

        if (jarFile) {
            command.parameters('-jar', jarFile)
        }

        command.parameters(parameters)
        command.interaction(GradleSpaceliftDelegate.ECHO_OUTPUT).execute().await()

        return null;
    }
}
