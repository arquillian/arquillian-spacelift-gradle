package org.arquillian.spacelift.gradle.android

import org.arquillian.spacelift.Spacelift
import org.arquillian.spacelift.process.ProcessInteraction
import org.arquillian.spacelift.process.ProcessInteractionBuilder
import org.arquillian.spacelift.task.Task

class AVDCreator extends Task<Object, Void> {

    private String target
    private String abi
    private String name
    private boolean force
    private boolean createSdCard
    private int sdCardSize = 128

    AVDCreator target(String target) {
        if (target && target.length() != 0) {
            this.target = target
        }
        this
    }

    AVDCreator abi(String abi) {
        if (abi && abi.length() != 0) {
            this.abi = abi
        }
        this
    }

    AVDCreator name(String name) {
        if (name && name.length() != 0) {
            this.name = name
        }
        this
    }

    AVDCreator force() {
        force = true
        this
    }

    AVDCreator sdCard(int size) {
        if (size > 0) {
            createSdCard = true
            sdCardSize = size
        }
        this
    }

    AVDCreator sdCard() {
        sdCard(sdCardSize)
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

        def tool = Spacelift.task("android")
                .parameters([
                    "create",
                    "avd",
                    "-n",
                    name,
                    "-t",
                    target,
                ]).interaction(interaction)

        if (createSdCard) {
            tool.parameters(["-c", sdCardSize + "M" ])
        }

        if (force) {
            tool.parameter("--force")
        }

        if (abi) {
            tool.parameters(["--abi", abi])
        }

        tool.execute().await()
        return null
    }
}
