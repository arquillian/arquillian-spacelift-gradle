package org.arquillian.spacelift.gradle.maven

import groovy.transform.CompileStatic

import org.arquillian.spacelift.Spacelift
import org.arquillian.spacelift.gradle.BaseContainerizableObject
import org.arquillian.spacelift.gradle.DeferredValue
import org.arquillian.spacelift.gradle.GradleSpaceliftDelegate
import org.arquillian.spacelift.gradle.Installation
import org.arquillian.spacelift.process.CommandBuilder
import org.arquillian.spacelift.task.Task
import org.arquillian.spacelift.task.TaskFactory
import org.arquillian.spacelift.task.TaskRegistry
import org.arquillian.spacelift.task.archive.UnzipTool
import org.arquillian.spacelift.task.net.DownloadTool
import org.arquillian.spacelift.task.os.CommandTool
import org.slf4j.Logger

@CompileStatic
class MavenInstallation extends BaseContainerizableObject<MavenInstallation> implements Installation {

    DeferredValue<String> product = DeferredValue.of(String.class).from("maven")

    DeferredValue<String> version = DeferredValue.of(String.class).from("3.3.3")

    DeferredValue<Boolean> isInstalled = DeferredValue.of(Boolean.class).from({ getHome().exists() })

    DeferredValue<File> home = DeferredValue.of(File.class).from("maven")

    DeferredValue<Void> postActions = DeferredValue.of(Void.class)

    DeferredValue<String> remoteUrl = DeferredValue.of(String.class).from({ "http://www.eu.apache.org/dist/maven/maven-3/${version}/binaries/apache-maven-${version}-bin.zip" })

    DeferredValue<String> fileName = DeferredValue.of(String.class).from({ "apache-maven-${version}-bin.zip" })

    DeferredValue<String> alias = DeferredValue.of(String.class).from("mvn")

    DeferredValue<Map> environment = DeferredValue.of(Map.class).from([:])

    MavenInstallation(String name, Object parent) {
        super(name, parent)
    }

    MavenInstallation(String name, MavenInstallation other) {
        super(name, other)

        this.product = other.@product.copy()
        this.version = other.@version.copy()
        this.isInstalled = other.@isInstalled.copy()
        this.home = other.@home.copy()
        this.postActions = other.@postActions.copy()
        this.remoteUrl = other.@remoteUrl.copy()
        this.fileName = other.@fileName.copy()
        this.alias = other.@alias.copy()
        this.environment = other.@environment.copy()
    }

    @Override
    public MavenInstallation clone(String name) {
        return new MavenInstallation(name, this)
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
    public boolean isInstalled() {
        isInstalled.resolve()
    }

    @Override
    public File getHome() {
        new File(home.resolve(), "apache-maven-" + getVersion())
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

    public Map<String, String> getEnvironment() {
        environment.resolve()
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
        Spacelift.task(getFsPath(),UnzipTool).toDir(((File)getHome()).parentFile.canonicalFile).execute().await()

        println getHome()

        new GradleSpaceliftDelegate().project().getAnt().invokeMethod("chmod", [dir: "${getHome()}/bin", perm:"a+x", includes:"*"])
    }

    @Override
    public void registerTools(TaskRegistry registry) {

        final String mavenAlias = getAlias()

        registry.register(MavenTool, new TaskFactory(){
            Task create() {
                Task task = new MavenTool()
                task.executionService = Spacelift.service()
                return task
            }

            Collection aliases() { [ mavenAlias ]}
        })
    }

    private File getFsPath() {
        return new File((File) parent['installationsDir'], "${getProduct()}/${getVersion()}/${getFileName()}")
    }

    private Map<String, String> getMavenEnvironmentProperties() {
        Map<String, String> envProperties = new HashMap<String, String>()

        envProperties.put("M2_HOME", getHome().canonicalPath)
        envProperties.put("M2", new File(getHome().canonicalPath, "bin").canonicalPath)
        envProperties.put("MAVEN_OPTS", "-Xms256m -Xmx512m")
        envProperties.putAll(getEnvironment())
        envProperties
    }

    class MavenTool extends CommandTool {

        DeferredValue<List> nativeCommand = DeferredValue.of(List).ownedBy(this).from([
                linux: { ["${home}/bin/mvn"]},
                mac: { ["${home}/bin/mvn"]},
                windows: {
                    [
                            "cmd.exe",
                            "/C",
                            "${home}/bin/mvn.cmd"
                    ]},
                solaris: {[ "${home}/bin/mvn" ]}
        ])

        MavenTool() {
            super()
            List command = nativeCommand.resolve()
            def quietParameters = System.getenv("TRAVIS") != null ? [ "-B", "-q" ] : []
            this.commandBuilder = new CommandBuilder(command as CharSequence[]).parameters(quietParameters)
            this.interaction = GradleSpaceliftDelegate.ECHO_OUTPUT

            // we need to use 'super' here, because it means we want to access
            // environment property of CommandTool, we have environment property
            // in MavenInstallation class as well and using 'this' would
            // result in an attempt to cast MavenTool to MavenInstallation which fails

            super.environment.putAll(MavenInstallation.this.getMavenEnvironmentProperties())
        }

        @Override
        public String toString() {
            return "MavenTool (${commandBuilder})"
        }
    }
}
