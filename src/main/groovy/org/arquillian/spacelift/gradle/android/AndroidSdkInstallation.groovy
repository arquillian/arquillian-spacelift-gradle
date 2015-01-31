package org.arquillian.spacelift.gradle.android

import groovy.transform.CompileStatic

import org.arquillian.spacelift.execution.Tasks
import org.arquillian.spacelift.gradle.BaseContainerizableObject
import org.arquillian.spacelift.gradle.DSLUtil
import org.arquillian.spacelift.gradle.DefaultGradleSpaceliftTaskFactory
import org.arquillian.spacelift.gradle.GradleSpacelift
import org.arquillian.spacelift.gradle.GradleSpaceliftTaskFactory
import org.arquillian.spacelift.gradle.InheritanceAwareContainer
import org.arquillian.spacelift.gradle.Installation
import org.arquillian.spacelift.process.CommandBuilder
import org.arquillian.spacelift.process.impl.CommandTool
import org.arquillian.spacelift.tool.ToolRegistry
import org.arquillian.spacelift.tool.basic.DownloadTool
import org.arquillian.spacelift.tool.basic.UntarTool
import org.arquillian.spacelift.tool.basic.UnzipTool
import org.gradle.api.Project
import org.slf4j.Logger

@CompileStatic
class AndroidSdkInstallation extends BaseContainerizableObject<AndroidSdkInstallation> implements Installation {

    Closure product = { "android" }

    Closure version = { "24.0.2" }

    Closure androidTargets = {}

    Closure updateSdk = { true }

    Closure isInstalled = {
        return getHome().exists()
    }

    Closure createAvds = { false }

    Closure postActions = {}

    Map remoteUrl = [
        linux: { "http://dl.google.com/android/android-sdk_r${version}-linux.tgz" },
        windows: { "http://dl.google.com/android/android-sdk_r${version}-windows.zip" },
        mac: { "http://dl.google.com/android/android-sdk_r${version}-macosx.zip" },
        solaris: { "http://dl.google.com/android/android-sdk_r${version}-linux.tgz" }
    ]

    Map home = [
        linux: "android-sdk-linux",
        windows:"android-sdk-windows",
        mac: "android-sdk-macosx",
        solaris: "android-sdk-linux"
    ]

    Map fileName = [
        linux: { "android-sdk_r${version}-linux.tgz" },
        windows: { "android-sdk_r${version}-windows.zip" },
        mac: { "android-sdk_r${version}-macosx.zip" },
        solaris: { "android-sdk_r${version}-linux.tgz" }
    ]

    // tools provided by this installation
    InheritanceAwareContainer<GradleSpaceliftTaskFactory, DefaultGradleSpaceliftTaskFactory> tools

    AndroidSdkInstallation(String name, Project project) {
        super(name, project)
        this.tools = new InheritanceAwareContainer(project, this, GradleSpaceliftTaskFactory, DefaultGradleSpaceliftTaskFactory)
    }

    AndroidSdkInstallation(String name, AndroidSdkInstallation other) {
        super(name, other)

        this.version = (Closure) other.@version.clone()
        this.product = (Closure) other.@product.clone()
        this.androidTargets = (Closure) other.@androidTargets.clone()
        this.isInstalled = (Closure)other.@isInstalled.clone()
        this.createAvds = (Closure)other.@createAvds.clone()
        this.postActions = (Closure) other.@postActions.clone()
        this.tools = (InheritanceAwareContainer<GradleSpaceliftTaskFactory, DefaultGradleSpaceliftTaskFactory>) other.@tools.clone()
    }

    @Override
    public AndroidSdkInstallation clone(String name) {
        return new AndroidSdkInstallation(name, this)
    }

    @Override
    String getVersion() {
        def versionString = DSLUtil.resolve(String.class, version, this)
        if(versionString==null) {
            return ""
        }

        return versionString.toString();
    }

    @Override
    String getProduct() {
        def productName = DSLUtil.resolve(String.class, product, this)
        if(productName==null) {
            return ""
        }

        return productName.toString()
    }

    @Override
    public boolean isInstalled() {
        return DSLUtil.resolve(Boolean.class, isInstalled, this)
    }

    @Override
    public void registerTools(ToolRegistry registry) {
        registry.register(AndroidTool)
        registry.register(AndroidAdbTool)
        registry.register(AndroidEmulatorTool)

        ((Iterable<GradleSpaceliftTaskFactory>) tools).each { GradleSpaceliftTaskFactory factory ->
            factory.register(registry)
        }
    }

    @Override
    public File getHome() {
        String homeDir = DSLUtil.resolve(String.class, DSLUtil.deferredValue(home), this)
        return new File((File)project['spacelift']['workspace'], homeDir)
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
            Tasks.prepare(DownloadTool).from(getRemoteUrl()).timeout(60000).to(targetFile).execute().await()
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
                Tasks.chain(getFsPath(),UnzipTool).toDir((File)project['spacelift']['workspace']).execute().await()
                break
            case ~/.*tgz/:
            case ~/.*tar\.gz/:
                Tasks.chain(getFsPath(),UntarTool).toDir((File)project['spacelift']['workspace']).execute().await()
                break
            default:
                logger.warn(":install:${name} Unable to extract ${getFileName()}, unknown archive type")
        }


        // we need to fix executable flags as Java Unzip does not preserve them
        project.getAnt().invokeMethod("chmod", [dir: "${getHome()}/tools", perm:"a+x", includes:"*", excludes:"*.txt"])

        // register tools from installation
        registerTools(GradleSpacelift.toolRegistry())

        // update Android SDK, download / update each specified Android SDK version
        if(getUpdateSdk()) {
            getAndroidTargets().each { AndroidTarget androidTarget ->
                Tasks.prepare(AndroidSdkUpdater).target(androidTarget.name).execute().await()
            }
        }

        // opt out for stats
        Tasks.prepare(AndroidSdkOptForStats).execute().await()

        if (getCreateEmulators()) {
            // create AVDs
            getAndroidTargets().each { AndroidTarget androidTarget ->
                String avdName = androidTarget.name
                AVDCreator avdcreator = Tasks.prepare(AVDCreator).target(avdName).name(avdName.replaceAll("\\W", "")).force()

                // set abi if it was defined
                if(androidTarget.abi) {
                    avdcreator.abi(androidTarget.abi)
                }

                avdcreator.execute().await()
            }
        }

        // execute post actions
        DSLUtil.resolve(postActions, this)
    }

    public String getRemoteUrl() {
        return DSLUtil.resolve(String.class, DSLUtil.deferredValue(remoteUrl), this)
    }

    public String getFileName() {
        return DSLUtil.resolve(String.class, DSLUtil.deferredValue(fileName), this)
    }

    public List<AndroidTarget> getAndroidTargets() {
        List<Object> targets = DSLUtil.resolve(List.class, androidTargets, this)

        return targets.collect { Object it -> new AndroidTarget(it) }
    }

    public boolean getCreateEmulators() {
        return DSLUtil.resolve(Boolean.class, createAvds, this)
    }

    public Boolean getUpdateSdk() {
        return DSLUtil.resolve(Boolean.class, updateSdk, this)
    }

    private File getFsPath() {
        return new File((File) project['spacelift']['installationsDir'], "${getProduct()}/${getVersion()}/${getFileName()}")
    }

    static class AndroidAdbTool extends CommandTool {

        Map nativeCommand = [
            linux: { ["${home}/platform-tools/adb"]},
            mac: { ["${home}/platform-tools/adb"]},
            windows: {
                [
                    "cmd.exe",
                    "/C",
                    "${home}/platform-tools/adb.exe"
                ]},
            solaris: {[
                    "${home}/platform-tools/adb"
                ]}
        ]

        // FIXME this is something what should be provided by spacelift
        private AndroidSdkInstallation sdk

        @Override
        protected Collection<String> aliases() {
            return ["adb"]
        }

        AndroidAdbTool() {
            super()
            // FIXME Spacelift does not support instantiation of inner classes - because of constructor parameter, we need to go through project
            // This will be most likely fixed in both depends/provides and task factories
            this.sdk = (AndroidSdkInstallation) GradleSpacelift.currentProject()['spacelift']['installations']
                    .find { it -> AndroidSdkInstallation.class.isAssignableFrom(it.getClass())}
            List command = DSLUtil.resolve(List.class, DSLUtil.deferredValue(nativeCommand), sdk, this, this)
            this.commandBuilder = new CommandBuilder(command as CharSequence[])
            this.interaction = GradleSpacelift.ECHO_OUTPUT
        }

        @Override
        public String toString() {
            return "AndroidAdbTool" + DSLUtil.resolve(List.class, DSLUtil.deferredValue(nativeCommand), sdk, this, this)
        }
    }

    static class AndroidEmulatorTool extends CommandTool {

        Map nativeCommand = [
            linux: { ["${home}/tools/emulator"]},
            mac: { ["${home}/tools/emulator"]},
            windows: {
                [
                    "cmd.exe",
                    "/C",
                    "${home}/tools/emulator.exe"
                ]},
            solaris: {["${home}/tools/emulator"]}
        ]

        // FIXME this is something what should be provided by spacelift
        private AndroidSdkInstallation sdk

        @Override
        protected Collection<String> aliases() {
            return ["emulator"]
        }

        AndroidEmulatorTool() {
            super()
            // FIXME Spacelift does not support instantiation of inner classes - because of constructor parameter, we need to go through project
            // This will be most likely fixed in both depends/provides and task factories
            this.sdk = (AndroidSdkInstallation) GradleSpacelift.currentProject()['spacelift']['installations']
                    .find { it -> AndroidSdkInstallation.class.isAssignableFrom(it.getClass())}
            List command = DSLUtil.resolve(List.class, DSLUtil.deferredValue(nativeCommand), sdk, this, this)
            this.commandBuilder = new CommandBuilder(command as CharSequence[])
            this.interaction = GradleSpacelift.ECHO_OUTPUT
        }

        @Override
        public String toString() {
            return "AndroidEmulatorTool" + DSLUtil.resolve(List.class, DSLUtil.deferredValue(nativeCommand), sdk, this, this)
        }
    }

    static class AndroidTool extends CommandTool {

        Map nativeCommand = [
            linux: { ["${home}/tools/android"]},
            mac: { ["${home}/tools/android"]},
            windows: {
                [
                    "cmd.exe",
                    "/C",
                    "${home}/tools/android.bat"
                ]},
            solaris: {[
                    "${home}/tools/android" ]}
        ]

        // FIXME this is something what should be provided by spacelift
        private AndroidSdkInstallation sdk

        @Override
        protected Collection<String> aliases() {
            return ["android"]
        }

        AndroidTool() {
            super()
            // FIXME Spacelift does not support instantiation of inner classes - because of constructor parameter, we need to go through project
            // This will be most likely fixed in both depends/provides and task factories
            this.sdk = (AndroidSdkInstallation) GradleSpacelift.currentProject()['spacelift']['installations']
                    .find { it -> AndroidSdkInstallation.class.isAssignableFrom(it.getClass())}
            List command = DSLUtil.resolve(List.class, DSLUtil.deferredValue(nativeCommand), sdk, this, this)
            this.commandBuilder = new CommandBuilder(command as CharSequence[])
            this.interaction = GradleSpacelift.ECHO_OUTPUT
        }

        @Override
        public String toString() {
            return "AndroidTool" + DSLUtil.resolve(List.class, DSLUtil.deferredValue(nativeCommand), sdk, this, this)
        }
    }

    private static class AndroidTarget {
        final String name
        final String abi

        AndroidTarget(Object object) {
            if(object instanceof Map) {
                Map map = (Map) object
                if(!map.containsKey("name")) {
                    throw new IllegalArgumentException("Android target definition must contain name")
                }
                this.name = map.name
                this.abi = map.abi
            }
            else {
                this.name = object.toString()
            }
        }

        @Override
        public String toString() {
            return "AndroidTarget: ${name}" + ((abi)? " - ${abi}" :"")
        }
    }

}
