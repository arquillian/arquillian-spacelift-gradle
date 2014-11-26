package org.arquillian.spacelift.gradle.android

import org.arquillian.spacelift.execution.Task
import org.arquillian.spacelift.gradle.GradleSpacelift
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class AndroidSdkOptForStats extends Task {

    private static final Logger log = LoggerFactory.getLogger('AndroidSdkOptForStats')

    private def androidSdkHomes = []

    def AndroidSdkOptForStats addAndroidSdkHome(File androidSdkHome) {
        if (androidSdkHome) {
            addAndroidSdkHome(androidSdkHome.absolutePath)
        }
        this
    }

    def AndroidSdkOptForStats addAndroidSdkHome(String androidSdkHome) {
        if (androidSdkHome && !androidSdkHome.isEmpty()) {
            this.androidSdkHomes.add(androidSdkHome)
        }
        this
    }

    @Override
    protected Object process(Object input) throws Exception {

        if (!androidSdkHomes.contains(defaultAndroidSdkHome().absolutePath)) {
            androidSdkHomes.add(defaultAndroidSdkHome().absolutePath)
        }

        androidSdkHomes.each { androidSdkHome ->
            if(androidSdkHome) {
                log.info("Opting out for statistics coverage in ${androidSdkHome}/.android")
                try {
                    def dir = new File(new File(androidSdkHome), ".android")
                    dir.mkdirs()
                    // on purpose, we are destroying previous content of the file
                    new File(dir, "ddms.cfg").withWriter { writer ->
                        writer.println("pingOptIn=false")
                    }
                }
                catch (IOException e) {
                    // ignore this
                }
            }
        }

        null
    }

    // ANDROID_SDK_HOME
    // in this directory where .android is created
    File defaultAndroidSdkHome() {
        GradleSpacelift.currentProject().spacelift.workspace
    }

}
