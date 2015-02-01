package org.arquillian.spacelift.gradle.android

import org.arquillian.spacelift.gradle.GradleSpaceliftDelegate
import org.arquillian.spacelift.task.Task
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class AndroidSdkOptForStats extends Task<Object, Void> {

    private static final Logger log = LoggerFactory.getLogger('AndroidSdkOptForStats')

    private def androidSdkHomes = []

    AndroidSdkOptForStats addAndroidSdkHome(File androidSdkHome) {
        if (androidSdkHome) {
            addAndroidSdkHome(androidSdkHome.canonicalPath)
        }
        this
    }

    AndroidSdkOptForStats addAndroidSdkHome(String androidSdkHome) {
        if (androidSdkHome && !androidSdkHome.isEmpty()) {
            this.androidSdkHomes.add(androidSdkHome)
        }
        this
    }

    @Override
    protected Void process(Object input) throws Exception {

        if (!androidSdkHomes.contains(defaultAndroidSdkHome().canonicalPath)) {
            androidSdkHomes.add(defaultAndroidSdkHome().canonicalPath)
        }

        androidSdkHomes.each {
            androidSdkHome ->
            if(androidSdkHome) {
                log.info("Opting out for statistics coverage in ${androidSdkHome}/.android")
                try {
                    def dir = new File(new File(androidSdkHome), ".android")
                    dir.mkdirs()
                    // on purpose, we are destroying previous content of the file
                    new File(dir, "ddms.cfg").withWriter { writer ->
                        writer.println("pingId=")
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
        new GradleSpaceliftDelegate().project().spacelift.workspace
    }

}
