package org.arquillian.spacelift.gradle

import java.lang.reflect.Field

import org.arquillian.spacelift.Spacelift
import org.gradle.api.Project
import org.slf4j.Logger
import org.slf4j.LoggerFactory


/**
 * Defines a default configuration for Arquillian Spacelift Gradle Plugin
 *
 */
class SpaceliftExtension {
    private static final Logger logger = LoggerFactory.getLogger(SpaceliftExtension)

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
    InheritanceAwareContainer<GradleTask, DefaultGradleTask> tools
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
        this.profiles = new InheritanceAwareContainer(this, Profile, Profile)
        this.tools = new InheritanceAwareContainer(this, GradleTask, DefaultGradleTask)
        this.installations = new InheritanceAwareContainer(this, Installation, DefaultInstallation)
        this.tests = new InheritanceAwareContainer(this, Test, DefaultTest)
    }

    SpaceliftExtension profiles(Closure closure) {
        profiles.configure(closure)
        return this
    }

    SpaceliftExtension tools(Closure closure) {
        tools.configure(closure)

        // register existing tools
        tools.each { GradleTask task ->
            Spacelift.registry().register(task.factory())
        }

        return this
    }

    SpaceliftExtension installations(Closure closure) {
        installations.configure(closure)
        return this
    }

    SpaceliftExtension tests(Closure closure) {
        tests.configure(closure)
        return this
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

    @Override
    public String toString() {
        return "SpaceliftExtension" + (project.buildFile ? "(${project.buildFile.canonicalPath})" : "")
    }


    /**
     * If property was not found, try to check content of container for resolution
     * @param name Name of the property, can be any object defined in DSL
     * @return
     */
     def propertyMissing(String name) {

        // try all containers to find resolution in particular order
        //order here defines order of reference in case reference to the same object is found, laters are ignored
        def object, objectType
        for(def container : ([installations, tests, tools, profiles])) {
            def resolved = resolve(container, name)
            if(object==null && resolved != null) {
                logger.debug("Resolved ${container.type.getSimpleName()} named ${name}")
                object = resolved
                objectType = container.type
            }
            else if(object!=null && resolved != null) {
                logger.warn("Detected ambiguous reference ${name}, using ${objectType.getSimpleName()}, ignoring ${container.type.getSimpleName()}")
            }
        }

        if(object!=null) {
            return object
        }

        // pass resolution to parent
        throw new MissingPropertyException("Unable to resolve property named ${name} in Spacelift DSL")
    }

    private <TYPE extends ContainerizableObject<TYPE>, DEFAULT_TYPE extends TYPE> TYPE resolve(InheritanceAwareContainer<TYPE, DEFAULT_TYPE> container, String name) {
        try {
            return container.getAt(name)
        }
        catch(MissingPropertyException e) {
            logger.debug("Unable to resolve ${container.type.getSimpleName()} named ${name}")
            return null
        }
    }
}
