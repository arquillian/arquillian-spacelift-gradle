package org.arquillian.spacelift.gradle.android

import org.arquillian.spacelift.Spacelift
import org.arquillian.spacelift.process.ProcessResult
import org.arquillian.spacelift.task.Task

class AndroidEmulatorStartedChecker extends Task<Object, ProcessResult> {

    private String device = System.getenv("ANDROID_SERIAL") ?: "emulator-5554"

    def device(String device) {
        if (device && device.trim().length() != 0) {
            this.device = device.trim()
        }
        this
    }

    @Override
    protected ProcessResult process(Object input) throws Exception {
        Spacelift.task("adb")
                .parameters([
                    "-s",
                    device,
                    "-e",
                    "shell",
                    "getprop",
                    "init.svc.bootanim"
                ])
                .shouldExitWith(0,255)
                .execute().await()
    }

}
