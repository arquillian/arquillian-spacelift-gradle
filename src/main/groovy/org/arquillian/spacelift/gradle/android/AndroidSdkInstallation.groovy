package org.arquillian.spacelift.gradle.android

import groovy.transform.CompileStatic

import org.arquillian.spacelift.Spacelift
import org.arquillian.spacelift.gradle.BaseContainerizableObject
import org.arquillian.spacelift.gradle.DSLUtil
import org.arquillian.spacelift.gradle.DefaultGradleTask
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

    Closure buildTools = { "21.1.2" }
    
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
    InheritanceAwareContainer<GradleTask, DefaultGradleTask> tools

    AndroidSdkInstallation(String name, Project project) {
        super(name, project)
        this.tools = new InheritanceAwareContainer(project, this, GradleTask, DefaultGradleTask)
    }

    AndroidSdkInstallation(String name, AndroidSdkInstallation other) {
        super(name, other)

        this.version = (Closure) other.@version.clone()
        this.product = (Closure) other.@product.clone()
        this.androidTargets = (Closure) other.@androidTargets.clone()
        this.isInstalled = (Closure)other.@isInstalled.clone()
        this.createAvds = (Closure)other.@createAvds.clone()
        this.postActions = (Closure) other.@postActions.clone()
        this.tools = (InheritanceAwareContainer<GradleTask, DefaultGradleTask>) other.@tools.clone()
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
    public void registerTools(TaskRegistry registry) {
        registry.register(AndroidAdbTool, new TaskFactory(){
                    Task create() {
                        Task task = new AndroidAdbTool()
                        // FIXME inject execution service via private field
                        task.executionService = Spacelift.service()
                        return task;
                    }
                    Collection aliases() { ["adb"]}
                })
        registry.register(AndroidTool, new TaskFactory(){
            Task create() {
                Task task = new AndroidTool()
                // FIXME inject execution service via private field
                task.executionService = Spacelift.service()
                return task;
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
                Spacelift.task(getFsPath(),UnzipTool).toDir((File)project['spacelift']['workspace']).execute().await()
                break
            case ~/.*tgz/:
            case ~/.*tar\.gz/:
                Spacelift.task(getFsPath(),UntarTool).toDir((File)project['spacelift']['workspace']).execute().await()
                break
            default:
                logger.warn(":install:${name} Unable to extract ${getFileName()}, unknown archive type")
        }


        // we need to fix executable flags as Java Unzip does not preserve them
        project.getAnt().invokeMethod("chmod", [dir: "${getHome()}/tools", perm:"a+x", includes:"*", excludes:"*.txt"])

        // register tools from installation
        registerTools(Spacelift.registry())

        // update Android SDK, download / update each specified Android SDK version
        if(getUpdateSdk()) {
            getAndroidTargets().each { AndroidTarget androidTarget ->
                Spacelift.task(AndroidSdkUpdater).buildTools(getBuildTools()).target(androidTarget.name).execute().await()
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

    public boolean getCreateAvds() {
        return DSLUtil.resolve(Boolean.class, createAvds, this)
    }

    public String getBuildTools() {
        return DSLUtil.resolve(String.class, buildTools, this)
    }
    
    public Boolean getUpdateSdk() {
        return DSLUtil.resolve(Boolean.class, updateSdk, this)
    }

    private File getFsPath() {
        return new File((File) project['spacelift']['installationsDir'], "${getProduct()}/${getVersion()}/${getFileName()}")
    }

    private Map<String, String> getAndroidEnvironmentProperties() {
        Map<String, String> envProperties = new HashMap<String, String>();
        
        File androidHome = getHome()
        
        envProperties.put("ANDROID_HOME", androidHome.canonicalPath)
        envProperties.put("ANDROID_SDK_HOME", androidHome.parentFile.canonicalPath)
        envProperties.put("ANDROID_TOOLS", new File(androidHome, "tools").canonicalPath)
        envProperties.put("ANDROID_PLATFORM_TOOLS", new File(androidHome, "platform-tools").canonicalPath)
        
        return envProperties
    }
    
    class AndroidAdbTool extends CommandTool {

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

        AndroidAdbTool() {
            super()
            List command = DSLUtil.resolve(List.class, DSLUtil.deferredValue(nativeCommand), this, this)
            this.commandBuilder = new CommandBuilder(command as CharSequence[])
            this.interaction = GradleSpaceliftDelegate.ECHO_OUTPUT
            this.environment.putAll(AndroidSdkInstallation.this.getAndroidEnvironmentProperties())
        }

        @Override
        public String toString() {
            return "AndroidAdbTool" + DSLUtil.resolve(List.class, DSLUtil.deferredValue(nativeCommand), this, this)
        }
    }

    class AndroidEmulatorTool extends CommandTool {

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

        AndroidEmulatorTool() {
            super()
            List command = DSLUtil.resolve(List.class, DSLUtil.deferredValue(nativeCommand), this, this)
            this.commandBuilder = new CommandBuilder(command as CharSequence[])
            this.interaction = GradleSpaceliftDelegate.ECHO_OUTPUT
            this.environment.putAll(AndroidSdkInstallation.this.getAndroidEnvironmentProperties())
        }

        @Override
        public String toString() {
            return "AndroidEmulatorTool" + DSLUtil.resolve(List.class, DSLUtil.deferredValue(nativeCommand), this, this)
        }
    }

    class AndroidTool extends CommandTool {

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

        AndroidTool() {
            super()
            List command = DSLUtil.resolve(List.class, DSLUtil.deferredValue(nativeCommand), this, this)
            this.commandBuilder = new CommandBuilder(command as CharSequence[])
            this.interaction = GradleSpaceliftDelegate.ECHO_OUTPUT
            this.environment.putAll(AndroidSdkInstallation.this.getAndroidEnvironmentProperties())
        }

        @Override
        public String toString() {
            return "AndroidTool" + DSLUtil.resolve(List.class, DSLUtil.deferredValue(nativeCommand), this, this)
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
