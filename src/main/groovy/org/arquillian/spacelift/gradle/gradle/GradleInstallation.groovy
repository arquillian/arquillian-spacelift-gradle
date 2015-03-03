package org.arquillian.spacelift.gradle.gradle

import java.io.File;
import java.util.Collection;
import java.util.List;

import groovy.transform.CompileStatic

import org.arquillian.spacelift.Spacelift
import org.arquillian.spacelift.gradle.BaseContainerizableObject
import org.arquillian.spacelift.gradle.DefaultGradleTask
import org.arquillian.spacelift.gradle.DeferredValue
import org.arquillian.spacelift.gradle.GradleSpaceliftDelegate;
import org.arquillian.spacelift.gradle.GradleTask
import org.arquillian.spacelift.gradle.InheritanceAwareContainer
import org.arquillian.spacelift.gradle.Installation
import org.arquillian.spacelift.process.CommandBuilder;
import org.arquillian.spacelift.task.Task
import org.arquillian.spacelift.task.TaskFactory
import org.arquillian.spacelift.task.TaskRegistry
import org.arquillian.spacelift.task.archive.UnzipTool
import org.arquillian.spacelift.task.archive.UntarTool
import org.arquillian.spacelift.task.net.DownloadTool
import org.arquillian.spacelift.task.os.CommandTool
import org.slf4j.Logger

@CompileStatic
class GradleInstallation extends BaseContainerizableObject<GradleInstallation> implements Installation {

    DeferredValue<String> product = DeferredValue.of(String.class).from("gradle")

    DeferredValue<String> version = DeferredValue.of(String.class).from("2.3")

    DeferredValue<Boolean> isInstalled = DeferredValue.of(Boolean.class).from({ getHome().exists() })

    DeferredValue<String> remoteUrl = DeferredValue.of(String.class).from({ "https://services.gradle.org/distributions/gradle-${version}-bin.zip" })

    DeferredValue<String> fileName = DeferredValue.of(String.class).from({ "gradle-${version}-bin.zip" })

    DeferredValue<File> home = DeferredValue.of(File.class).from("gradleInstallation")

    DeferredValue<Void> postActions = DeferredValue.of(Void.class)
    
    DeferredValue<String> alias = DeferredValue.of(String.class).from("gradle")

    // tools provided by this installation
    InheritanceAwareContainer<GradleTask, DefaultGradleTask> tools

    GradleInstallation(String name, Object parent) {
        super(name, parent)
        this.tools = new InheritanceAwareContainer(this, GradleTask, DefaultGradleTask)
    }

    GradleInstallation(String name, GradleInstallation other) {
        super(name, other)

        this.product = other.@product.copy()
        this.version = other.@version.copy()
        this.isInstalled = other.@isInstalled.copy()
        this.postActions = other.@postActions.copy()
        this.remoteUrl = other.@remoteUrl.copy()
        this.home = other.@home.copy()
        this.fileName = other.@fileName.copy()

        this.tools = (InheritanceAwareContainer<GradleTask, DefaultGradleTask>) other.@tools.clone()
    }

    @Override
    public GradleInstallation clone(String name) {
        return new GradleInstallation(name, this)
    }

    @Override
    public String getProduct() {
        product.resolve()
    }

    @Override
    public String getVersion() {
        version.resolve()
    }

    @Override
    public File getHome() {
        new File(home.resolve(), "gradle-" + getVersion())
    }

    @Override
    public boolean isInstalled() {
        isInstalled.resolve()
    }

    public String getRemoteUrl() {
        remoteUrl.resolve()
    }

    public String getFileName() {
        fileName.resolve()
    }

    public String getAlias() {
        alias.resolve()
    }
    
    @Override
    public void install(Logger logger) {

        File targetFile = getFsPath()

        if (targetFile.exists()) {
            logger.info(":install:${name} Grabbing ${getFileName()} from file system cache")
        } else if(getRemoteUrl()!=null){
            // ensure parent directory exists
            targetFile.getParentFile().mkdirs()

            // dowload bits if they do not exists
            logger.info(":install:${name} Grabbing from ${getRemoteUrl()}, storing at ${targetFile}")
            Spacelift.task(DownloadTool).from(getRemoteUrl()).timeout(120000).to(targetFile).execute().await()
        }

        logger.info(":install:${name} Extracting installation from ${getFileName()}")

        // based on installation type, we might want to unzip/untar/something else
        switch(getFileName()) {
            case ~/.*zip/:
                Spacelift.task(getFsPath(),UnzipTool).toDir(((File)getHome()).parentFile.canonicalFile).execute().await()
                break
            case ~/.*tgz/:
            case ~/.*tar\.gz/:
                Spacelift.task(getFsPath(),UntarTool).toDir(((File)getHome()).parentFile.canonicalFile).execute().await()
                break
            default:
                logger.warn(":install:${name} Unable to extract ${getFileName()}, unknown archive type")
        }

        new GradleSpaceliftDelegate().project().getAnt().invokeMethod("chmod", [dir: "${getHome()}/bin", perm:"a+x", includes:"*"])

        // register tools from installation
        registerTools(Spacelift.registry())
    }

    @Override
    public void registerTools(TaskRegistry registry) {
        
        final String gradleAlias = getAlias()
        
        registry.register(GradleTool, new TaskFactory(){
            Task create() {
                Task task = new GradleTool()
                task.executionService = Spacelift.service()
                return task
            }

            Collection aliases() { [ gradleAlias ]}
        })

        ((Iterable<GradleTask>) tools).each { GradleTask task ->
            Spacelift.registry().register(task.factory())
        }
    }

    private File getFsPath() {
        return new File((File) parent['installationsDir'], "${getProduct()}/${getVersion()}/${getFileName()}")
    }
    
    private Map<String, String> getGradleEnvironmentProperties() {
        Map<String, String> envProperties = new HashMap<String, String>()
        envProperties.put("GRADLE_HOME", getHome().canonicalPath)
        envProperties
    }

    private class GradleTool extends CommandTool {

        DeferredValue<List> nativeCommand = DeferredValue.of(List).ownedBy(this).from([
            linux: { ["${home}/bin/gradle"]},
            mac: { ["${home}/bin/gradle"]},
            windows: {
                [
                    "cmd.exe",
                    "/C",
                    "${home}/bin/gradle.bat"
                ]},
            solaris: {[ "${home}/bin/gradle" ]}
        ])

        GradleTool() {
            super()
            List command = nativeCommand.resolve()
            this.commandBuilder = new CommandBuilder(command as CharSequence[])
            this.interaction = GradleSpaceliftDelegate.ECHO_OUTPUT
            this.environment.putAll(GradleInstallation.this.getGradleEnvironmentProperties())
        }

        @Override
        public String toString() {
            return "GradleTool (${commandBuilder})"
        }
    }
}
