package org.arquillian.spacelift.gradle.android

import org.arquillian.spacelift.execution.Task
import org.arquillian.spacelift.gradle.GradleSpacelift

class AVDCreator extends Task<Object, Void> {

    private String target
    private String abi

    public AVDCreator target(String target) {
        this.target = target
        this
    }

    public AVDCreator abi(String abi) {
        this.abi = abi
        this
    }

    @Override
    protected Void process(Object input) throws Exception {
        def tool = GradleSpacelift.tools("android")
                .parameters(["create" ,"avd", "-n", target.replaceAll("\\W", ""), "-t", target, "--force"])
                .interaction(GradleSpacelift.ECHO_OUTPUT)

        if(abi) {
            tool.parameters("--abi", abi)
        }

        tool.execute().await()

        return null
    }
}
