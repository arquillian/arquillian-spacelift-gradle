package org.arquillian.spacelift.gradle.android

import org.arquillian.spacelift.execution.Task
import org.arquillian.spacelift.gradle.GradleSpacelift
import org.arquillian.spacelift.process.ProcessInteraction
import org.arquillian.spacelift.process.ProcessInteractionBuilder

class AVDCreator extends Task<Object, Void> {

    private String target
    private String abi
    private String name
    private boolean force

    def target(String target) {
        if (target && target.length() != 0) {
            this.target = target
        }
        this
    }

    def abi(String abi) {
        if (abi && abi.length() != 0) {
            this.abi = abi
        }
        this
    }

    def name(String name) {
        if (name && name.length() != 0) {
            this.name = name
        }
        this
    }

    def force() {
        force = true
        this
    }

    @Override
    protected Void process(Object input) throws Exception {

        if (!target) {
            throw new IllegalStateException("You have not set any target of Android AVD to create.")
        }

        if (!name) {
            throw new IllegalStateException("You have not set the name of to be created AVD.")
        }

        ProcessInteraction interaction = new ProcessInteractionBuilder()
                .outputPrefix("")
                .when("Do you wish to create a custom hardware profile \\[no\\]")
                .replyWith("no" + System.getProperty("line.separator"))
                .when("(?s).*").printToOut()
                .build()

        def tool = GradleSpacelift.tools("android")
                .parameters([
                    "create",
                    "avd",
                    "-n",
                    name,
                    "-t",
                    target,
                ]).interaction(interaction)

        if (force) {
            tool.parameter("--force")
        }

        if (abi) {
            tool.parameters(["--abi", abi])
        }

        tool.execute().await()

        null
    }
}
