package org.arquillian.spacelift.gradle.android

import groovy.transform.CompileStatic

import org.arquillian.spacelift.Spacelift
import org.arquillian.spacelift.gradle.BaseContainerizableObject
import org.arquillian.spacelift.gradle.DefaultGradleTask
import org.arquillian.spacelift.gradle.DeferredValue
import org.arquillian.spacelift.gradle.GradleSpaceliftDelegate
import org.arquillian.spacelift.gradle.GradleTask
import org.arquillian.spacelift.gradle.InheritanceAwareContainer
import org.arquillian.spacelift.gradle.Installation
import org.arquillian.spacelift.process.CommandBuilder
import org.arquillian.spacelift.task.Task
import org.arquillian.spacelift.task.TaskFactory
import org.arquillian.spacelift.task.TaskRegistry
import org.arquillian.spacelift.task.archive.UntarTool
import org.arquillian.spacelift.task.archive.UnzipTool
import org.arquillian.spacelift.task.net.DownloadTool
import org.arquillian.spacelift.task.os.CommandTool
import org.slf4j.Logger

@CompileStatic
class AndroidSdkInstallation extends BaseContainerizableObject<AndroidSdkInstallation> implements Installation {

    DeferredValue<String> product = DeferredValue.of(String.class).from("android")

    DeferredValue<String> version = DeferredValue.of(String.class).from("24.0.2")

    DeferredValue<String> buildTools = DeferredValue.of(String.class).from("21.1.2")

    DeferredValue<List> androidTargets = DeferredValue.of(List.class).from([])

    DeferredValue<Boolean> updateSdk = DeferredValue.of(Boolean.class).from(true)

    DeferredValue<Boolean> isInstalled = DeferredValue.of(Boolean.class).from({
        return getHome().exists()
    })

    DeferredValue<Boolean> createAvds = DeferredValue.of(Boolean.class).from(true)

    DeferredValue<Boolean> updateImages = DeferredValue.of(Boolean.class).from(false)

    // actions to be invoked after installation is done
    DeferredValue<Void> postActions = DeferredValue.of(Void.class)

    DeferredValue<String> remoteUrl = DeferredValue.of(String.class).from([
        linux: { "http://dl.google.com/android/android-sdk_r${version}-linux.tgz" },
        windows: { "http://dl.google.com/android/android-sdk_r${version}-windows.zip" },
        mac: { "http://dl.google.com/android/android-sdk_r${version}-macosx.zip" },
        solaris: { "http://dl.google.com/android/android-sdk_r${version}-linux.tgz" }
    ])

    DeferredValue<File> home = DeferredValue.of(File.class).from([
        linux: "android-sdk-linux",
        windows:"android-sdk-windows",
        mac: "android-sdk-macosx",
        solaris: "android-sdk-linux"
    ])

    DeferredValue<String> fileName = DeferredValue.of(String.class).from([
        linux: { "android-sdk_r${version}-linux.tgz" },
        windows: { "android-sdk_r${version}-windows.zip" },
        mac: { "android-sdk_r${version}-macosx.zip" },
        solaris: { "android-sdk_r${version}-linux.tgz" }
    ])

    // tools provided by this installation
    InheritanceAwareContainer<GradleTask, DefaultGradleTask> tools

    AndroidSdkInstallation(String name, Object parent) {
        super(name, parent)
        this.tools = new InheritanceAwareContainer(this, GradleTask, DefaultGradleTask)
    }

    AndroidSdkInstallation(String name, AndroidSdkInstallation other) {
        super(name, other)

        this.version = other.@version.copy()
        this.product = other.@product.copy()
        this.isInstalled = other.@isInstalled.copy()
        this.postActions = other.@postActions.copy()
        this.remoteUrl = other.@remoteUrl.copy()
        this.home = other.@home.copy()
        this.fileName = other.@fileName.copy()

        this.androidTargets = other.@androidTargets.copy()
        this.createAvds = other.@createAvds.copy()
        this.updateSdk = other.@updateSdk.copy()
        this.updateImages = other.@updateImages.copy()

        this.tools = (InheritanceAwareContainer<GradleTask, DefaultGradleTask>) other.@tools.clone()
    }

    @Override
    public AndroidSdkInstallation clone(String name) {
        return new AndroidSdkInstallation(name, this)
    }

    @Override
    String getVersion() {
        return version.resolve()
    }

    @Override
    String getProduct() {
        return product.resolve()
    }

    @Override
    public boolean isInstalled() {
        return isInstalled.resolve()
    }

    @Override
    public void registerTools(TaskRegistry registry) {
        registry.register(AndroidAdbTool, new TaskFactory(){
                    Task create() {
                        Task task = new AndroidAdbTool()
                        // FIXME inject execution service via private field
                        task.executionService = Spacelift.service()
                        return task
                    }
                    Collection aliases() { ["adb"]}
                })
        registry.register(AndroidTool, new TaskFactory(){
                    Task create() {
                        Task task = new AndroidTool()
                        // FIXME inject execution service via private field
                        task.executionService = Spacelift.service()
                        return task
                    }
                    Collection aliases() { ["android"]}
                })
        registry.register(AndroidEmulatorTool, new TaskFactory() {
                    Task create() {
                        Task task = new AndroidEmulatorTool()
                        // FIXME inject execution service via private field
                        task.executionService = Spacelift.service()
                        return task
                    }
                    Collection aliases() {["emulator"]}
                })

        ((Iterable<GradleTask>) tools).each { GradleTask task ->
            Spacelift.registry().register(task.factory())
        }
    }

    @Override
    public File getHome() {
        return home.resolve()
    }

    @Override
    public void install(Logger logger) {

        File targetFile = getFsPath()
        if(targetFile.exists()) {
            logger.info(":install:${name} Grabbing ${getFileName()} from file system cache")
        }
        else if(getRemoteUrl()!=null){
            // ensure parent directory exists
            targetFile.getParentFile().mkdirs()

            // dowload bits if they do not exists
            logger.info(":install:${name} Grabbing from ${getRemoteUrl()}, storing at ${targetFile}")
            Spacelift.task(DownloadTool).from(getRemoteUrl()).timeout(60000).to(targetFile).execute().await()
        }

        // extract file if set to and at the same time file is defined
        if(getHome().exists()) {
            logger.info(":install:${name} Deleting previous installation at ${getHome()}")
            //project.ant.delete(dir: getHome())
        }

        logger.info(":install:${name} Extracting installation from ${getFileName()}")

        // based on installation type, we might want to unzip/untar/something else
        switch(getFileName()) {
            case ~/.*zip/:
                Spacelift.task(getFsPath(),UnzipTool).toDir((File)parent['workspace']).execute().await()
                break
            case ~/.*tgz/:
            case ~/.*tar\.gz/:
                Spacelift.task(getFsPath(),UntarTool).toDir((File)parent['workspace']).execute().await()
                break
            default:
                logger.warn(":install:${name} Unable to extract ${getFileName()}, unknown archive type")
        }


        // we need to fix executable flags as Java Unzip does not preserve them
        // FIXME
        new GradleSpaceliftDelegate().project().getAnt().invokeMethod("chmod", [dir: "${getHome()}/tools", perm:"a+x", includes:"*", excludes:"*.txt"])

        // register tools from installation
        registerTools(Spacelift.registry())

        // update Android SDK, download / update each specified Android SDK version
        if(getUpdateSdk()) {

            // update platform first
            logger.info("Updating platform.")
            Spacelift.task(AndroidSdkUpdater)
                .updatePlatform(true)
                .buildTools(getBuildTools())
                .execute().await()

            // then update tools and platform for every target
            getAndroidTargets()
                .collect(new HashSet<String>(), { AndroidTarget androidTarget -> "android-" + androidTarget.getApiLevel().toString()})
                .each { String target ->
                    logger.info("Updating platform for target: " + target)
                    Spacelift.task(AndroidSdkUpdater)
                        .target(target)
                        .buildTools(getBuildTools())
                        .execute().await()
                }

            // and then update unique system images
            if (getUpdateImages()) {
                getAndroidTargets()
                    .collect(new HashSet<String>(), { AndroidTarget target -> target.images })
                    .flatten()
                    .unique()
                    .each { image ->
                        logger.info("Updating system images for target: " + image)
                        Spacelift.task(AndroidSdkUpdater)
                            .image((String) image)
                            .execute().await()
                    }
            }

            // opt out for stats
            Spacelift.task(AndroidSdkOptForStats).execute().await()

            if (getCreateAvds()) {
                // create AVDs
                getAndroidTargets().each { AndroidTarget androidTarget ->
                    String avdName = androidTarget.name
                    AVDCreator avdcreator = Spacelift.task(AVDCreator).target(avdName).name(avdName.replaceAll("\\W", "")).force()

                    // set abi if it was defined
                    if(androidTarget.abi) {
                        avdcreator.abi(androidTarget.abi)
                    }

                    avdcreator.execute().await()
                }
            }

            // execute post actions
            postActions.resolve()
        }
    }

    public String getRemoteUrl() {
        return remoteUrl.resolve()
    }

    public String getFileName() {
        return fileName.resolve()
    }

    public List<AndroidTarget> getAndroidTargets() {
        List<Object> targets = androidTargets.resolve()
        return targets.collect { Object it -> new AndroidTarget(it) }
    }

    public boolean getCreateAvds() {
        return createAvds.resolve()
    }

    public String getBuildTools() {
        return buildTools.resolve()
    }

    public Boolean getUpdateSdk() {
        return updateSdk.resolve()
    }

    public boolean getUpdateImages() {
        return updateImages.resolve()
    }

    private File getFsPath() {
        return new File((File) parent['installationsDir'], "${getProduct()}/${getVersion()}/${getFileName()}")
    }

    private Map<String, String> getAndroidEnvironmentProperties() {
        Map<String, String> envProperties = new HashMap<String, String>()

        File androidHome = getHome()

        envProperties.put("ANDROID_HOME", androidHome.canonicalPath)
        envProperties.put("ANDROID_SDK_HOME", ((File) parent['workspace']).canonicalPath)
        envProperties.put("ANDROID_SDK_ROOT", androidHome.canonicalPath)
        envProperties.put("ANDROID_TOOLS", new File(androidHome, "tools").canonicalPath)
        envProperties.put("ANDROID_PLATFORM_TOOLS", new File(androidHome, "platform-tools").canonicalPath)

        return envProperties
    }

    class AndroidAdbTool extends CommandTool {

        // set owner via API
        DeferredValue<List> nativeCommand = DeferredValue.of(List).ownedBy(this).from([
            linux: { ["${home}/platform-tools/adb"]},
            mac: { ["${home}/platform-tools/adb"]},
            windows: {
                [
                    "cmd.exe",
                    "/C",
                    "${home}/platform-tools/adb.exe"
                ]},
            solaris: {[ "${home}/platform-tools/adb" ]}
        ])

        AndroidAdbTool() {
            super()
            List command = nativeCommand.resolve()
            this.commandBuilder = new CommandBuilder(command as CharSequence[])
            this.interaction = GradleSpaceliftDelegate.ECHO_OUTPUT
            this.environment.putAll(AndroidSdkInstallation.this.getAndroidEnvironmentProperties())
        }

        @Override
        public String toString() {
            return "AndroidAdbTool (${commandBuilder})"
        }
    }

    class AndroidEmulatorTool extends CommandTool {

        // set owner via API
        DeferredValue<List> nativeCommand = DeferredValue.of(List).ownedBy(this).from([
            linux: { ["${home}/tools/emulator"]},
            mac: { ["${home}/tools/emulator"]},
            windows: {
                [
                    "cmd.exe",
                    "/C",
                    "${home}/tools/emulator.exe"
                ]},
            solaris: {["${home}/tools/emulator"]}
        ])

        AndroidEmulatorTool() {
            super()
            List command = nativeCommand.resolve()
            this.commandBuilder = new CommandBuilder(command as CharSequence[])
            this.interaction = GradleSpaceliftDelegate.ECHO_OUTPUT
            this.environment.putAll(AndroidSdkInstallation.this.getAndroidEnvironmentProperties())
        }

        @Override
        public String toString() {
            return "AndroidEmulatorTool (${commandBuilder})"
        }
    }

    class AndroidTool extends CommandTool {

        // set owner via API
        DeferredValue<List> nativeCommand = DeferredValue.of(List).ownedBy(this).from([
            linux: { ["${home}/tools/android"]},
            mac: { ["${home}/tools/android"]},
            windows: {
                [
                    "cmd.exe",
                    "/C",
                    "${home}/tools/android.bat"
                ]},
            solaris: {[ "${home}/tools/android"
                ]}
        ])

        AndroidTool() {
            super()
            List command = nativeCommand.resolve()
            this.commandBuilder = new CommandBuilder(command as CharSequence[])
            this.interaction = GradleSpaceliftDelegate.ECHO_OUTPUT
            this.environment.putAll(AndroidSdkInstallation.this.getAndroidEnvironmentProperties())
        }

        @Override
        public String toString() {
            return "AndroidTool (${commandBuilder})"
        }
    }

    private static class AndroidTarget {
        final String name
        final String abi
        final List<String> images = new ArrayList<String>()

        AndroidTarget(Object object) {
            if(object instanceof Map) {
                Map map = (Map) object
                if(!map.containsKey("name")) {
                    throw new IllegalArgumentException("Android target definition must contain name")
                }
                this.name = map.name
                this.abi = map.abi

                List<String> images = map.images as List<String>

                if (images) {
                    this.images.addAll(images)
                }
            }
            else {
                this.name = object.toString()
            }
        }

        public Integer getApiLevel() {
            if(name.isInteger()) {
                return name.toInteger()
            }

            def androidVersionCandidates = name.tokenize('-')
            if(androidVersionCandidates.last().isInteger()) {
                return androidVersionCandidates.last().toInteger()
            }

            androidVersionCandidates = name.tokenize(':')
            if(androidVersionCandidates.last().isInteger()) {
                return androidVersionCandidates.last().toInteger()
            }
        }

        public String getAbi() {
            abi
        }

        public List<String> images() {
            return images
        }

        @Override
        public String toString() {
            return "AndroidTarget: ${name}" + ((abi)? " - ${abi}" :"" ) + ((images)? "- ${images}" : "")
        }
    }
}
