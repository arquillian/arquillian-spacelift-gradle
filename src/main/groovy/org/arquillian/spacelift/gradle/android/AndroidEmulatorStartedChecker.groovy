package org.arquillian.spacelift.gradle.android

import java.util.Collection

import org.arquillian.spacelift.execution.ExecutionCondition
import org.arquillian.spacelift.execution.Task
import org.arquillian.spacelift.execution.Tasks
import org.arquillian.spacelift.gradle.GradleSpacelift
import org.arquillian.spacelift.process.ProcessResult
import org.arquillian.spacelift.process.impl.CommandTool
import org.arquillian.spacelift.tool.Tool

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
        GradleSpacelift.tools("adb")
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
