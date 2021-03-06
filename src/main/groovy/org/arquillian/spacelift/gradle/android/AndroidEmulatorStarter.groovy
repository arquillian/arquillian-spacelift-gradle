package org.arquillian.spacelift.gradle.android

import java.util.concurrent.TimeUnit

import org.arquillian.spacelift.Spacelift
import org.arquillian.spacelift.execution.CountDownWatch
import org.arquillian.spacelift.execution.Execution
import org.arquillian.spacelift.execution.ExecutionCondition
import org.arquillian.spacelift.process.ProcessResult
import org.arquillian.spacelift.task.Task

class AndroidEmulatorStarter extends Task<Object, Execution<ProcessResult>> {

    private static final ExecutionCondition<Boolean> EMULATOR_STARTED_CONDITION = new AndroidEmulatorStarter.AndroidEmulatorStartedCondition()

    private String avd

    private String port = "5554"

    private List<String> emulatorParameters = new ArrayList<String>()

    private int timeout = 300 // in seconds

    def avd(String avd) {
        if (avd && avd.length() != 0) {
            this.avd = avd
        }
        this
    }

    def port(String port) {
        int p = port.toInteger()
        if (p >= 5554 && p <= 5584) {
            this.port = port
        }
        this
    }

    def port(int port) {
        this.port(port.toString())
    }

    def parameters(List<String> parameters) {
        if (parameters) {
            emulatorParameters = parameters
        }
        this
    }

    def timeout(int timeout) {
        if (timeout > 0) {
            this.timeout = timeout
        }
        this
    }

    def timeout(String timeout) {
        this.timeout(timeout.toInteger())
    }

    @Override
    protected Execution<ProcessResult> process(Object input) throws Exception {

        if (!avd) {
            throw new IllegalStateException("avd to start was not speficied")
        }

        Spacelift.task("emulator").parameters(["-avd", avd, "-port", port]).parameters(emulatorParameters).execute()

        Spacelift.task(AndroidEmulatorStartedChecker)
                .device("emulator-" + port)
                .execute().until(new CountDownWatch(timeout, TimeUnit.SECONDS), EMULATOR_STARTED_CONDITION)

        Spacelift.task(UnlockEmulatorTask).device("emulator-" + port).execute().await()
    }

    private static final class AndroidEmulatorStartedCondition implements ExecutionCondition<ProcessResult> {

        @Override
        boolean satisfiedBy(ProcessResult result) {

            if (!result) {
                return false
            }

            boolean found

            // looking for boot animation stopping
            result.output().each { line ->
                if (line && line.contains("stopped")) {
                    found = true
                }
            }

            found
        }
    }
}
