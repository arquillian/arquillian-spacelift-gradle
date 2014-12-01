package org.arquillian.spacelift.gradle.cordova

import org.arquillian.spacelift.execution.Task
import org.arquillian.spacelift.execution.Tasks
import org.arquillian.spacelift.gradle.GradleSpacelift
import org.arquillian.spacelift.process.CommandBuilder
import org.arquillian.spacelift.process.ProcessResult
import org.arquillian.spacelift.process.impl.CommandTool

class CordovaExecutor extends Task<Object, ProcessResult> {

    def parameters
    def dir
    Map<String,String> env = [:]
    def sdkHome

    def setParameters(parameters) {
        this.parameters = parameters
        this
    }

    def setDir(dir) {
        this.dir = dir
        this
    }

    def androidHome(androidHome) {
        this.env << [ANDROID_HOME:androidHome.toString()]
        this
    }

    def androidSdkHome(androidSdkHome) {
        this.env << [ANDROID_SDK_HOME:androidSdkHome.toString()]
        this.sdkHome = androidSdkHome
        this
    }

    def env(key, value) {
        this.env.put(key.toString(), value.toString())
        this
    }

    def env(envProperty) {
        for(def entry:envProperty) {
            this.env.put(entry.key.toString(), entry.value.toString())
        }
        this
    }

    @Override
    protected ProcessResult process(Object input) throws Exception {
        def sep = System.getProperty("path.separator")

        // FIXME this should rely on Spacelift registry to get Cordova tool

        return Tasks.prepare(CommandTool)
                .command(new CommandBuilder("cordova").parameters(parameters.split(" ")))
                .workingDir(dir)
                .addEnvironment(env)
                .addEnvironment("PATH", "${sdkHome}/tools${sep}${sdkHome}/platform-tools${sep}${System.getenv("PATH")}")
                .interaction(GradleSpacelift.ECHO_OUTPUT)
                .shouldExitWith(0,1)
                .execute().await()
    }
}
