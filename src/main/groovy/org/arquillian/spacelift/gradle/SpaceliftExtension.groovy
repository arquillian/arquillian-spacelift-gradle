package org.arquillian.spacelift.gradle

import org.gradle.api.Project


/**
 * Defines a default configuration for Arquillian Spacelift Gradle Plugin
 *
 */
class SpaceliftExtension {

    // workspace configuration
    File workspace

    // base path to get local binaries
    File installationsDir

    // flag to kill already running servers
    boolean killServers

    // local Maven repository
    File localRepository

    // keystore and truststore files
    File keystoreFile
    File truststoreFile

    // staging JBoss repository
    boolean enableStaging

    // snapshots JBoss repository
    boolean enableSnapshots

    // internal DSL
    InheritanceAwareContainer<Profile, Profile> profiles
    InheritanceAwareContainer<GradleSpaceliftTaskFactory, DefaultGradleSpaceliftTaskFactory> tools
    InheritanceAwareContainer<Installation, DefaultInstallation> installations
    InheritanceAwareContainer<Test, DefaultTest> tests

    Project project

    SpaceliftExtension(Project project) {
        this.workspace = project.rootDir
        this.installationsDir = new File(workspace, "installations")

        this.localRepository = new File(workspace, ".repository")
        this.keystoreFile = new File(project.rootDir, "patches/certs/aerogear.keystore")
        this.truststoreFile = new File(project.rootDir, "patches/certs/aerogear.truststore")
        this.enableStaging = false
        this.enableSnapshots = false
        this.project = project
        this.profiles = new InheritanceAwareContainer(project, this, Profile, Profile)
        this.tools = new InheritanceAwareContainer(project, this, GradleSpaceliftTaskFactory, DefaultGradleSpaceliftTaskFactory)
        this.installations = new InheritanceAwareContainer(project, this, Installation, DefaultInstallation)
        this.tests = new InheritanceAwareContainer(project, this, Test, DefaultTest)
    }

    def profiles(Closure closure) {
        profiles.configure(closure)
        this
    }

    InheritanceAwareContainer<Profile, Profile> getProfiles() {
        profiles
    }

    def tools(Closure closure) {
        tools.configure(closure)

        // register existing tools
        tools.each { factory ->
            factory.register(GradleSpacelift.toolRegistry())
        }

        this
    }

    InheritanceAwareContainer<GradleSpaceliftTaskFactory, DefaultGradleSpaceliftTaskFactory> getTools() {
        tools
    }

    def installations(Closure closure) {
        installations.configure(closure)
        this
    }

    InheritanceAwareContainer<Installation, DefaultInstallation> getInstallations() {
        installations
    }

    def tests(Closure closure) {
        tests.configure(closure)
        this
    }

    InheritanceAwareContainer<Test, DefaultTest> getTests() {
        tests
    }


    def setWorkspace(workspace) {
        // update also dependant repositories when workspace is updated
        if(localRepository.parentFile == this.workspace) {
            this.localRepository = new File(workspace, ".repository")
        }
        // update also dependant repositories when workspace is updated
        if(installationsDir.parentFile == this.workspace) {
            this.installationsDir = new File(workspace, "installations")
        }

        this.workspace = workspace
    }
}
