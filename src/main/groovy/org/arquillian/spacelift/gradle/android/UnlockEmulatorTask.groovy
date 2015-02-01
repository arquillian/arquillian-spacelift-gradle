package org.arquillian.spacelift.gradle.android

import org.arquillian.spacelift.Spacelift
import org.arquillian.spacelift.task.Task
import org.arquillian.spacelift.task.os.CommandTool

class UnlockEmulatorTask extends Task<Object, Void> {

    private String device = System.getenv("ANDROID_SERIAL") ?: "emulator-5554"

    UnlockEmulatorTask device(String device) {
        if (device && device.length() != null) {
            this.device = device
        }
        this
    }

    @Override
    protected Void process(Object input) throws Exception {

        CommandTool ct = Spacelift.task("adb")

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
