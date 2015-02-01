package org.arquillian.spacelift.gradle.android

import org.arquillian.spacelift.Spacelift
import org.arquillian.spacelift.process.ProcessInteractionBuilder
import org.arquillian.spacelift.task.Task
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class AndroidSdkUpdater extends Task<Object, Void>{

    static final Logger log = LoggerFactory.getLogger('AndroidSdkUpdater')

    private String target

    //
    // The version of build-tools is statically set to 21.1.1 - the latest one by 23/11/2014.
    // Investigate, how to get the latest build-tools version programmatically.
    //
    // Backed by JIRA: https://issues.jboss.org/browse/MP-209
    //
    private String buildToolsVersion = "21.1.1"

    AndroidSdkUpdater target(String target) {
        if (target && !target.isEmpty()) {
            this.target = target
        }
        this
    }

    AndroidSdkUpdater buildTools(String buildToolsVersion) {
        if (buildToolsVersion && !buildToolsVersion.isEmpty()) {
            this.buildToolsVersion = buildToolsVersion
        }
        this
    }

    @Override
    protected Void process(Object ignored) throws Exception {

        log.info("Checking installed Android target: ${target}")

        // first, check whether Android version is already installed
        def availableTargets = Spacelift.task('android').parameters([
            "list",
            "target",
            "-c"
        ])
        .interaction(new ProcessInteractionBuilder()
        .when('^(?!Error).*$').printToErr())
        .execute().await().output()

        if(availableTargets.find { "${it}" == "${target}"}) {
            log.info("There was already Android ${target} installed, skipping update")
            return
        }

        log.info("Updating Android SDK to contain version ${target}")

        // android version we need was not installed, let's install it
        // use "android list sdk --all --extended" to get what should be installed
        def androidVersion = androidVersionInteger(target)

        def exitCodes = 0..255

        // additional packages to be update based on Android version selected externally
        def androidVersionSpecificPackages = "android-${androidVersion},addon-google_apis-google-${androidVersion},addon-google_apis_x86-google-${androidVersion}";
        // handle system images, installing only x86 platform
        if(target.contains("Google APIs")) {
            androidVersionSpecificPackages += ",sys-img-x86-addon-google_apis-google-${androidVersion}"
        }
        else {
            androidVersionSpecificPackages += ",sys-img-x86-android-${androidVersion}"
        }

        Spacelift.task('android').parameters([
            "update",
            "sdk",
            "--filter",
            "platform-tools," +
            "build-tools-" + buildToolsVersion + "," +
            "extra-google-google_play_services," +
            "extra-android-support," +
            androidVersionSpecificPackages,
            "--all",
            "--no-ui"]
        ).interaction(new ProcessInteractionBuilder()
        .outputPrefix("")
        .when('^Do you accept the license.*: ').replyWith('y')
        .when('^\\s*Done.*installed\\.$').terminate()
        .when('^(?!Error).*$').printToOut()
        .when('^!Error.*$').printToErr()
        )
        .shouldExitWith(exitCodes.toArray(new Integer[0]))
        .execute().await()

        log.info("Android SDK was updated.")
    }

    private Integer androidVersionInteger(androidTarget) {

        if(androidTarget.isInteger()) {
            return androidTarget.toInteger()
        }

        def androidVersionCandidates = androidTarget.tokenize('-')
        if(androidVersionCandidates.last().isInteger()) {
            return androidVersionCandidates.last().toInteger()
        }

        androidVersionCandidates = androidTarget.tokenize(':')
        if(androidVersionCandidates.last().isInteger()) {
            return androidVersionCandidates.last().toInteger()
        }

        throw new IllegalArgumentException("Android target expected in form android-XYZ or Google Inc.:Google APIs:XYZ")
    }
}
