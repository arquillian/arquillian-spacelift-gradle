package org.arquillian.spacelift.gradle.android

import java.io.File
import java.util.logging.Level
import java.util.logging.Logger

import org.arquillian.spacelift.execution.ExecutionException
import org.arquillian.spacelift.execution.Task
import org.arquillian.spacelift.execution.Tasks
import org.arquillian.spacelift.gradle.GradleSpacelift
import org.arquillian.spacelift.process.CommandBuilder
import org.arquillian.spacelift.process.impl.CommandTool

class UnlockEmulatorTask extends Task<Object, Void> {

    private String device = System.getenv("ANDROID_SERIAL") ?: "emulator-5554"

    def device(String device) {
        if (device && device.length() != null) {
            this.device = device
        }
        this
    }

    @Override
    protected Void process(Object input) throws Exception {

        CommandTool ct = GradleSpacelift.tools("adb")

        ct.parameters([
            "-s",
            device,
            "shell",
            "input",
            "keyevent",
            "82"
        ]).execute().await()

        ct.parameters([
            "-s",
            device,
            "shell",
            "input",
            "keyevent",
            "4"
        ]).execute().await()

        null
    }
}
