package org.arquillian.spacelift.gradle

import org.arquillian.spacelift.Spacelift
import org.arquillian.spacelift.gradle.configuration.ConfigurationContainer
import org.arquillian.spacelift.gradle.configuration.ConfigurationItem
import org.gradle.api.Project
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Defines a default configuration for Arquillian Spacelift Gradle Plugin
 *
 */
class SpaceliftExtension {
    private static final Logger logger = LoggerFactory.getLogger(SpaceliftExtension)

    // FIXME, workspace and cacheDir should be provided by Arquillian Spacelift itself
    // workspace configuration
    DeferredValue<File> workspace = DeferredValue.of(File).from({
        return new File(project.rootDir, "ws")
    })

    // base path to local cache, by default shared cache per all spacelift projects
    DeferredValue<File> cacheDir = DeferredValue.of(File).from({
        File cacheParentDir = new File(System.getProperty("user.home", "."))
        return new File(cacheParentDir, ".spacelift/cache")
    })

    // FIXME this should be better defined
    // flag to kill already running servers
    DeferredValue<Boolean> killServers = DeferredValue.of(Boolean).from(false)

    // internal DSL
    ConfigurationContainer configuration
    InheritanceAwareContainer<Profile, Profile> profiles
    InheritanceAwareContainer<GradleTask, DefaultGradleTask> tools
    InheritanceAwareContainer<Installation, DefaultInstallation> installations
    InheritanceAwareContainer<Test, DefaultTest> tests

    Project project

    SpaceliftExtension(Project project) {
        DSLInstrumenter.instrument(this)
        this.project = project
        this.configuration = new ConfigurationContainer(this)
        this.profiles = new InheritanceAwareContainer(this, Profile, Profile)
        this.tools = new InheritanceAwareContainer(this, GradleTask, DefaultGradleTask)
        this.installations = new InheritanceAwareContainer(this, Installation, DefaultInstallation)
        this.tests = new InheritanceAwareContainer(this, Test, DefaultTest)
    }

    SpaceliftExtension configuration(Closure closure) {
        configuration.configure(closure)
        return this
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

    SpaceliftExtension workspace(Object... lazyClosure) {
        workspace.from(lazyClosure)
        return this
    }

    def getWorkspace() {
        return workspace.resolve()
    }

    SpaceliftExtension cacheDir(Object... lazyClosure) {
        cacheDir.from(lazyClosure)
        return this
    }

    def getCacheDir() {
        return cacheDir.resolve()
    }

    SpaceliftExtension isKillServers(Object... lazyClosure) {
        killServers.from(lazyClosure)
        return this
    }

    def isKillServers() {
        return killServers.resolve()
    }

    @Override
    public String toString() {
        return "Spacelift " + (project.buildFile ? "(${project.buildFile.canonicalPath})" : "") +
                "${this.getProfiles()}"
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

        def containers = [configuration, installations, tests, tools, profiles]

        // FIXME We should not need to ask `project` for the selected profile (issue #50)
        if (project.hasProperty("selectedProfile")) {
            // The selected profile configuration needs to be first for it to override the main configuration
            containers.add(0, project.selectedProfile.configuration)
        }


        for (def container : containers) {
            def resolved = resolve(container, name)
            if (object == null && resolved != null) {
                logger.debug("Resolved ${container.type.getSimpleName()} named ${name}")
                if (resolved instanceof ConfigurationItem) {
                    object = resolved.getValue()
                    objectType = resolved.type.resolve()
                } else {
                    object = resolved
                    objectType = container.type
                }
            } else if (object != null && resolved != null) {
                logger.warn("Detected ambiguous reference ${name}, using ${objectType.getSimpleName()}, ignoring ${container.type.getSimpleName()}")
            }
        }

        if (object != null) {
            return object
        }
        // pass resolution to parent
        throw new MissingPropertyException("Unable to resolve property named ${name} in Spacelift DSL")
    }

    private <TYPE extends ContainerizableObject<TYPE>, DEFAULT_TYPE extends TYPE> TYPE resolve(InheritanceAwareContainer<TYPE, DEFAULT_TYPE> container, String name) {
        try {
            return container.getAt(name)
        }
        catch (MissingPropertyException e) {
            logger.debug("Unable to resolve ${container.type.getSimpleName()} named ${name}")
            return null
        }
    }
}
