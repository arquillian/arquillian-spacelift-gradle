package org.arquillian.spacelift.gradle.android

import org.arquillian.spacelift.Spacelift
import org.arquillian.spacelift.process.ProcessInteractionBuilder
import org.arquillian.spacelift.task.Task
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class AndroidSdkUpdater extends Task<Object, Void>{

    static final Logger log = LoggerFactory.getLogger('AndroidSdkUpdater')

    private String target

    private String image

    private boolean updatePlatform = false

    //
    // The version of build-tools is statically set to 21.1.1 - the latest one by 23/11/2014.
    // Investigate, how to get the latest build-tools version programmatically.
    //
    // Backed by JIRA: https://issues.jboss.org/browse/MP-209
    //
    private String buildToolsVersion = "21.1.2"

    AndroidSdkUpdater target(String target) {
        if (target && !target.isEmpty()) {
            this.target = target
        }
        this
    }

    AndroidSdkUpdater image(String image) {
        if (image && !image.isEmpty()) {
            this.image = image
        }
        this
    }

    AndroidSdkUpdater updatePlatform(boolean updatePlatform) {
        this.updatePlatform = updatePlatform
        this
    }

    AndroidSdkUpdater updateImages(boolean updateImages) {
        this.updateImages = updateImages
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

        // update just core platform and build tools
        if (updatePlatform) {

            log.info("Updating platform tools and build tools: platform-tools,build-tools-" + buildToolsVersion
                    + ",extra-google-google_play_services,extra-android-support")

            Spacelift.task('android').parameters([
                "update",
                "sdk",
                "--filter",
                "platform-tools," +
                "build-tools-" + buildToolsVersion + "," +
                "extra-google-google_play_services,extra-android-support",
                "--all",
                "--no-ui"]
            ).interaction(new ProcessInteractionBuilder()
            .outputPrefix("")
            .when('^Do you accept the license.*: ').replyWith('y')
            .when('^\\s*Done.*installed\\.$').terminate()
            .when('^(?!Error).*$').printToOut()
            .when('^!Error.*$').printToErr())
            .shouldExitWith((0..255).toArray(new Integer[0]))
            .execute().await()
        }

        // update tool and platform tool for some target
        if (target) {

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

            log.info("Updating platform tool and platform-tool for target android-" + androidVersionInteger(target))

            Spacelift.task('android').parameters([
                "update",
                "sdk",
                "--filter",
                "android-" + androidVersionInteger(target) + ",platform-tool,tool",
                "--all",
                "--no-ui"]
            ).interaction(new ProcessInteractionBuilder()
            .outputPrefix("")
            .when('^Do you accept the license.*: ').replyWith('y')
            .when('^\\s*Done.*installed\\.$').terminate()
            .when('^(?!Error).*$').printToOut()
            .when('^!Error.*$').printToErr())
            .shouldExitWith((0..255).toArray(new Integer[0]))
            .execute().await()
        }

        // update / download images
        if (image) {
            log.info("AndroidSdkUpdater downloads " + image)
            Spacelift.task('android').parameters([
                "update",
                "sdk",
                "--filter",
                image,
                "--all",
                "--no-ui"]
            ).interaction(new ProcessInteractionBuilder()
            .outputPrefix("")
            .when('^Do you accept the license.*: ').replyWith('y')
            .when('^\\s*Done.*installed\\.$').terminate()
            .when('^(?!Error).*$').printToOut()
            .when('^!Error.*$').printToErr())
            .shouldExitWith((0..255).toArray(new Integer[0]))
            .execute().await()
        }

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
