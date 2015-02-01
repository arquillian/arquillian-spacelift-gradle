package org.arquillian.spacelift.gradle.utils

import org.arquillian.spacelift.Spacelift
import org.arquillian.spacelift.process.CommandBuilder
import org.arquillian.spacelift.task.Task
import org.arquillian.spacelift.task.os.CommandTool

class RHELVersion extends Task<Object, String> {

    @Override
    protected String process(Object input) throws Exception {

        def version = "no RHEL"

        if (!EnvironmentUtils.runsOnLinux()) {
            return version
        }

        def rhelVersion = Spacelift.task(CommandTool).command(new CommandBuilder('cat'))
                .parameters('/etc/redhat-release')
                .shouldExitWith(0,1)
                .execute()
                .await()
                .output()

        if (!rhelVersion.isEmpty()) {
            if (rhelVersion.get(0).contains("Red Hat")) {
                if (rhelVersion.get(0).contains(" 6")) {
                    version = "6"
                } else if (rhelVersion.get(0).contains(" 7")) {
                    version = "7"
                }
            }
        }

        version
    }
}
