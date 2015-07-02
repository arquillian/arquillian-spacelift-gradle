package org.arquillian.spacelift.gradle

import org.arquillian.spacelift.Spacelift
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
            logger.lifecycle(":install:${installation.name} will be installed")
            installation.install(logger)
            logger.lifecycle(":install:${installation.name} is now installed, registering tools")
        }
        installation.registerTools(Spacelift.registry())
    }


    // this plugin prepares Arquillian Spacelift environment
    void apply(Project project) {

        // set current project reference
        GradleSpaceliftDelegate.currentProject(project)

        project.extensions.create("spacelift", SpaceliftExtension, project)

        // set current project and initialize tools
        // this task make following properties available
        // * project.selectedProfile
        // * project.selectedInstallations
        // * project.selectedTests
        project.getTasks().create(name:"init") { task ->
            description "Initialize Spacelift environment"
            group "Spacelift"
            task << {
                logger.lifecycle(":init:defaultValues")

                // find default profile and propagate enabled installations and tests
                // check for -Pprofile=profileName and then for Mavenism -PprofileName
                // fallback to default if not defined
                def profileName = 'default'
                if(project.hasProperty('profile')){
                    profileName = project.profile
                }
                else {
                    try {
                        // try to find profile by Maven based -PprofileName
                        profileName = project.spacelift.profiles.find { p -> project.hasProperty(p.name) }.name
                    }
                    catch(NullPointerException e) {
                        logger.warn(":init: Please select profile by -Pprofile=name, now using default")
                    }
                }

                // find if such profile is available
                def profile = project.spacelift.profiles.find { p -> p.name == profileName }
                if(profile==null) {
                    def availableProfiles = project.spacelift.profiles.collect { it.name }.join(', ')
                    throw new GradleException("Unable to find ${profileName} profile in build.gradle file, available profiles were: ${availableProfiles}")
                }

                // make selected profile global
                logger.lifecycle(":init:profile-" + profile.name)
                project.ext.set("selectedProfile", profile)

                logger.lifecycle(":init:configuration")
                loadConfiguration(project, logger)

                // find installations that were specified by profile or enabled manually from command line
                def installations = []
                def installationNames = []
                if(project.hasProperty('installations')) {
                    installationNames = project.installations.split(',').findAll { return !it.isEmpty() }
                }
                else if(profile.enabledInstallations==null) {
                    installationNames = []
                }
                else if(profile.enabledInstallations.contains('*')) {
                    installationNames = project.spacelift.installations.collect(new ArrayList()) {installation -> installation.name}
                }
                else {
                    installationNames = profile.enabledInstallations
                }
                installations = installationNames.inject(new ArrayList()) { list, installationName ->
                    def installation = project.spacelift.installations[installationName]
                    if(installation) {
                        logger.info(":init: Installation ${installationName} will be installed.")
                        list << installation
                        return list
                    }
                    logger.warn(":init: Selected installation ${installationName} does not exist and will be ignored")
                    return list
                }

                // make selected installations global
                project.ext.set("selectedInstallations", installations)

                // find tests that were specified by profile or enabled manually from command line
                def testNames = []
                if(project.hasProperty('tests')) {
                    testNames = project.tests.split(',').findAll { return !it.isEmpty() }
                }
                else if(profile.tests==null) {
                    testNames = []
                }
                else if(profile.tests.contains('*')) {
                    testNames = project.spacelift.tests.collect(new ArrayList()) {test -> test.name}
                }
                else {
                    testNames = profile.tests
                }

                def excludedTestNames = []
                if (project.hasProperty('excludedTests')) {
                    excludedTestNames = project.excludedTests.split(',').findAll { return !it.isEmpty() }
                }
                else if (profile.excludedTests == null) {
                    excludedTestNames = []
                }
                else {
                    excludedTestNames = profile.excludedTests
                }

                def testsToExecute = testNames - excludedTestNames

                def tests = testsToExecute.inject(new ArrayList()) { list, testName ->
                    def test = project.spacelift.tests[testName]
                    if(test) {
                        logger.info(":init: Selected test ${testName} will be tested (if task 'test' is run).")
                        list << test
                        return list
                    }
                    logger.warn(":init: Selected tests ${testName} does not exist and will be ignored")
                    return list
                }

                // make selected installations global
                project.ext.set("selectedTests", tests)
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

        project.getTasks().create(name:"assemble", dependsOn:["init"]) { task ->
            description "Fetches and installs environment required for test execution"
            group "Spacelift"
            task << {
                logger.lifecycle(":assemble:profile-${project.selectedProfile.name}")

                ant.mkdir(dir: "${project.spacelift.workspace}")

                if(project.spacelift.isKillServers()) {
                    Spacelift.task(KillJavas).execute().await()
                }

                project.selectedInstallations.each { Installation installation ->
                    installInstallation(installation, logger)
                }
            }
        }

        project.getTasks().create(name:"test", dependsOn:["assemble"]) { task ->
            description "Executes Spacelift defined tests"
            group "Spacelift"
            task << {
                project.selectedTests.each { test ->
                    logger.lifecycle(":test:test-${test.name} will be executed")
                    test.executeTest(logger)
                }
            }
        }

        project.getTasks().create(name:"testreport", dependsOn:["assemble"]) { task ->
            description "Collects all tests results into single JUnit report"
            group "Spacelift"
            task << {
                logger.lifecycle(":testreport:generating JUnit report for all tests in ${project.spacelift.workspace}")

                ant.mkdir(dir: "${project.spacelift.workspace}/test-reports")
                ant.mkdir(dir: "${project.spacelift.workspace}/test-reports/html")
                ant.taskdef(name: 'junitreport',
                classname: 'org.apache.tools.ant.taskdefs.optional.junit.XMLResultAggregator',
                classpath: project.configurations.junitreport.asPath)
                ant.junitreport(todir: "${project.spacelift.workspace}/test-reports") {
                    fileset(dir: "${project.spacelift.workspace}") {
                        include(name: "**/TEST*.xml")
                        exclude(name: "test-reports/*.xml")
                        exclude(name: "test-reports/html/*")
                    }
                    report(format: "noframes", todir: "${project.spacelift.workspace}/test-reports/html")
                }

                logger.lifecycle(":testreport:test report available in file://${project.spacelift.workspace}/test-reports/html/junit-noframes.html")
            }
        }

        project.getTasks().create(name:"cleanWorkspace", dependsOn:["init"]) { task ->
            description "Cleans Spacelift workspace"
            group "Spacelift"
            task << {
                logger.lifecycle(":clean:workspace will be wiped (${project.spacelift.workspace})")
                ant.delete(dir: project.spacelift.workspace, failonerror: false)
            }
        }

        project.getTasks().create(name:"cleanCache", dependsOn:["init"]) { task ->
            description "Cleans selected Spacelift installations from cache and installation location"
            group "Spacelift"
            task << {
                project.selectedInstallations.each { installation ->
                    logger.lifecycle(":clean:installation-${installation.name} artifacts (cache and home) will be wiped")
                    // delete installation cache
                    ant.delete(file: installation.fileName, failonerror: false)
                    // delete installation only if it is not workspace
                    if(installation.home.canonicalPath!=project.spacelift.workspace.canonicalPath) {
                        ant.delete(dir: installation.home, failonerror: false)
                    }
                }
            }
        }

        // task aliases and aggregators

        project.getTasks().create(name:"cleanAll", dependsOn:[
            "cleanCache",
            "cleanWorkspace",
        ]) {
            description "Cleans Spacelift workspace and selected installations from Spacelift cache and installation location"
            group "Spacelift"
        }

        project.getTasks().create(name:"check", dependsOn:["test"]) {
            description "Alias for test task"
            group "Spacelift"
        }

        project.getTasks().create(name:"prepare-env", dependsOn:["assemble"]) {
            description "Alias for assemble task"
            group "Spacelift"
        }
    }

    private void loadConfiguration(Project project, Object logger) {
        List<ConfigurationItem<?>> configurationItems = new ArrayList<>()

        // We want all configuration items from the selected profile
        configurationItems.addAll(project.selectedProfile.configuration)

        // We add properties that are not overridden in profile
        project.spacelift.configuration.each { ConfigurationItem<?> item ->
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
                    logger.lifecycle(":init:config ${item.name} was set to ${stringValue} from command line")
                }
            }
        }
    }
}
