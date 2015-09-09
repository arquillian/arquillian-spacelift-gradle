package org.arquillian.spacelift.gradle

import org.arquillian.spacelift.Spacelift
import org.arquillian.spacelift.gradle.configuration.ConfigurationContainer
import org.arquillian.spacelift.gradle.configuration.ConfigurationItem
import org.arquillian.spacelift.gradle.configuration.IllegalConfigurationException

import org.arquillian.spacelift.gradle.configuration.BuiltinConfigurationItemConverters
import org.arquillian.spacelift.gradle.utils.KillJavas
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger

class SpaceliftPlugin implements Plugin<Project> {

    /**
     * Static helper method to install an installation. It is used mainly to verify installation process in tests
     */
    static void installInstallation(Installation installation, Logger logger) {

        if(installation.isInstalled()) {
            logger.lifecycle(":install:${installation.name} was already installed, registering tools")
        }
        else {
            logger.lifecycle(":install:${installation.name} ...")
            installation.install(logger)
            logger.lifecycle(":install:${installation.name} is now installed, registering tools")
        }
        installation.registerTools(Spacelift.registry())
    }

    static void testTest(Test test, Logger logger) {
        logger.lifecycle(":test:${test.name} is now executed")
        test.executeTest(logger)
        logger.lifecycle(":test:${test.name} execution is finished")
    }


    // this plugin prepares Arquillian Spacelift environment
    void apply(Project project) {

        // set current project reference
        GradleSpaceliftDelegate.currentProject(project)

        SpaceliftExtension spacelift = project.extensions.create("spacelift", SpaceliftExtension, project)

        project.getTasks().create(name:"assemble") { assembleTask ->
            assembleTask << {

                ConfigurationContainer originalConfiguration = spacelift.configuration
                List<String> specifiedTasks = project.getGradle().startParameter.taskNames
                // if no profile was specified, run all installations and tests
                if(specifiedTasks.disjoint(project.spacelift.profiles.collect { it.name})) {

                    def (List<Installation> installations, List<Test> tests) = loadAll(project, null, originalConfiguration, logger)

                    // assemble phase
                    logger.lifecycle(":assemble")
                    ant.mkdir(dir: "${project.spacelift.workspace}")

                    if (project.spacelift.isKillServers()) {
                        Spacelift.task(KillJavas).execute().await()
                    }

                    installations.each { Installation installation ->
                        installInstallation(installation, logger)
                    }
                }

                // create profile tasks
                project.spacelift.profiles.each { Profile profile ->
                    project.getTasks().create(name: "${profile.name}") { task ->
                        description "${profile.description}"
                        group "Spacelift"
                        task << {

                            def (List<Installation> installations, List<Test> tests) = loadAll(project, profile, originalConfiguration, logger)

                            // assemble phase
                            logger.lifecycle(":assemble:${profile.name}")
                            ant.mkdir(dir: "${project.spacelift.workspace}")
                            if (project.spacelift.isKillServers()) {
                                Spacelift.task(KillJavas).execute().await()
                            }
                            installations.each { Installation installation ->
                                installInstallation(installation, logger)
                            }
                        }
                    }
                }
            }
        }


        project.getTasks().create(name:"test") { testTask ->
            testTask << {

                ConfigurationContainer originalConfiguration = spacelift.configuration
                List<String> specifiedTasks = project.getGradle().startParameter.taskNames
                // if no profile was specified, run all installations and tests
                if(specifiedTasks.disjoint(project.spacelift.profiles.collect { it.name})) {

                    def (List<Installation> installations, List<Test> tests) = loadAll(project, null, originalConfiguration, logger)

                    // assemble phase
                    logger.lifecycle(":assemble")
                    ant.mkdir(dir: "${project.spacelift.workspace}")

                    if (project.spacelift.isKillServers()) {
                        Spacelift.task(KillJavas).execute().await()
                    }
                    installations.each { Installation installation ->
                        installInstallation(installation, logger)
                    }
                    // test phase
                    tests.each { Test test ->
                        testTest(test, logger)
                    }
                }

                // create profile tasks
                project.spacelift.profiles.each { Profile profile ->
                    project.getTasks().create(name: "${profile.name}") { task ->
                        description "${profile.description}"
                        group "Spacelift"
                        task << {

                            def (List<Installation> installations, List<Test> tests) = loadAll(project, profile, originalConfiguration, logger)

                            // assemble phase
                            logger.lifecycle(":assemble:${profile.name}")
                            ant.mkdir(dir: "${project.spacelift.workspace}")
                            if (project.spacelift.isKillServers()) {
                                Spacelift.task(KillJavas).execute().await()
                            }
                            installations.each { Installation installation ->
                                installInstallation(installation, logger)
                            }

                            // test phase
                            tests.each { Test test ->
                                testTest(test, logger)
                            }
                        }
                    }
                }
            }
        }

        project.getTasks().create(name:"describe") { task ->
            description "Show more information about this Spacelift project"
            group "Spacelift"
            task << {
                println "Spacelift Workspace:          ${project.spacelift.workspace}"
                println "Spacelift Installations Cache Dir:  ${project.spacelift.cacheDir}"
                println "Spacelift configuration: \n${project.spacelift.configuration.toString()}"
                println "Spacelift Profiles: "

                project.spacelift.profiles.each { println it }
            }
        }


        project.getTasks().create(name:"clean") { task ->
            description "Cleans Spacelift workspace"
            group "Spacelift"
            task << {
                logger.lifecycle(":clean:workspace will be wiped (${project.spacelift.workspace})")
                ant.delete(dir: project.spacelift.workspace, failonerror: false)
            }
        }
    }

    private Object[] loadAll(Project project, Profile profile, ConfigurationContainer originalConfiguration, Object logger) {

        // make selected profile global, if any
        if(profile) {
            project.ext.set("selectedProfile", profile)
        }

        ConfigurationContainer configuration = loadConfiguration(project, profile, originalConfiguration, logger)
        List<Installation> installations = loadInstallations(project, profile, logger)
        List<Test> tests = loadTests(project, profile, logger)

        // this allows to invoke multiple profiles at the same time
        project.spacelift.configuration = configuration

        // FIXME this might not be needed - hopefully can be removed to make to increase encapsulation
        // make selected installations global
        project.ext.set("selectedInstallations", installations)
        // make selected installations global
        project.ext.set("selectedTests", tests)

        return [installations, tests]
    }

    private ConfigurationContainer loadConfiguration(Project project, Profile profile, ConfigurationContainer originalConfiguration, Object logger) {

        ConfigurationContainer configuration = originalConfiguration.clone()
        List<ConfigurationItem<?>> configurationItems = new ArrayList<>()

        // We want all configuration items from the selected profile
        if(profile) {
            configurationItems.addAll(profile.configuration.clone())
        }

        // We add properties that are not overridden in profile
        configuration.each { ConfigurationItem<?> item ->
            def profileItem = configurationItems.find { ConfigurationItem<?> profileItem ->
                profileItem.name.equals(item.name)
            }
            if(profileItem == null) {
                configurationItems.add(item);
            } else if(!item.type.resolve().isAssignableFrom(profileItem.type.resolve())) {
                throw IllegalConfigurationException.incompatibleOverride(item, profileItem)
            }
        }

        // Sorting alphabetically helps with log readability
        configurationItems.sort(false) { ConfigurationItem<?> a, ConfigurationItem<?> b ->
            a.name.compareTo(b.name)
        }.each { ConfigurationItem<?> item ->
            // If the project has property with this name, it was probably an input from the command line
            if(project.hasProperty(item.name)) {
                if(item.isSet()) {
                    throw new IllegalStateException("Value for property ${item.name} was already set in build.gradle file. You cannot override it.")
                } else {
                    def stringValue = project.property(item.name) as String

                    if(!item.isConverterSet()) {
                        item.converter.from(BuiltinConfigurationItemConverters.getConverter(item.type.resolve()))
                    }

                    def value = item.converter.resolve().fromString(stringValue)
                    item.value(value)
                    logger.lifecycle(":init:config ${item.name} was set to ${stringValue} ${item.getValue()} from command line")
                }
            }
        }
        return configuration
    }

    private List<Installation> loadInstallations(Project project, Profile profile, Object logger) {

        def installationNames = []
        // if profile is defined, use data from there
        if(profile) {
            installationNames = profile.enabledInstallations
        }
        else {
            if(project.hasProperty('installations')) {
                installationNames = project.installations.split(',').findAll { return !it.isEmpty() }
            }
            else {
                installationNames = ['*']
            }
        }

        if(installationNames.contains('*')) {
            installationNames = project.spacelift.installations.collect(new ArrayList()) {installation -> installation.name}
        }

        return installationNames.inject(new ArrayList()) { list, installationName ->
            def installation = project.spacelift.installations[installationName]
            if(installation) {
                list << installation
                return list
            }
            logger.warn(":init: installation ${installationName} does not exist and will be ignored")
            return list
        }
    }

    private List<Test> loadTests(Project project, Profile profile, Object logger) {

        def testNames = []

        // if profile is defined, use data from there
        if(profile) {
            testNames = profile.tests - profile.excludedTests
        }
        // create all or custom profile based on what user provided
        else {
            if(project.hasProperty('tests')) {
                testNames = project.tests.split(',').findAll { return !it.isEmpty() }
            }
            else {
                testNames = ['*']
            }
        }

        // apply wildcard
        if(testNames.contains('*')) {
            testNames = project.spacelift.tests.collect(new ArrayList()) { test -> test.name}
        }

        return testNames.inject(new ArrayList()) { list, testName ->
            def test = project.spacelift.tests[testName]
            if(test) {
                list << test
                return list
            }
            logger.warn(":init: test ${testName} does not exist and will be ignored")
            return list
        }
    }
}
