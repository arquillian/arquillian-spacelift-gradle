package org.arquillian.spacelift.gradle

import static org.hamcrest.CoreMatchers.*
import static org.junit.Assert.assertThat

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule;
import org.junit.Test
import org.junit.rules.ExpectedException;

class InheritanceTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    void "inherit installation properties"() {
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'spacelift'

        project.spacelift {
            installations {
                firstInstallation {
                    product "test"
                    version "1"
                    remoteUrl "https://github.com/arquillian/arquillian-selenium-bom/archive/master.zip"
                    home "mydirectory"
                    extractMapper {
                        toDir("${home}")
                        cutdirs()
                    }
                    postActions {
                        assertThat project, is(notNullValue())
                        assertThat project.spacelift, is(notNullValue())
                        assertThat project.spacelift.installationsDir, is(notNullValue())
                        assertThat "${home}".toString(), containsString("${project.spacelift.workspace}/mydirectory")
                        assertThat home.exists(), is(true)
                        assertThat new File(home, "pom.xml").exists(), is(true)
                    }
                }
                secondInstallation(inherits:"firstInstallation") {
                    home "mydirectory2"
                    postActions {
                        assertThat "${home}".toString(), containsString("${project.spacelift.workspace}/mydirectory2")
                        assertThat home.exists(), is(true)
                        assertThat new File(home, "pom.xml").exists(), is(true)
                    }
                }
            }
        }

        project.spacelift.installations.each { installation ->
            installation.install(project.logger)
        }
    }

    @Test
    void "inherit installation direct reference"() {
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'spacelift'
        project.spacelift {
            installations {
                firstInstallation {
                    product "test"
                    version "1"
                    remoteUrl "https://github.com/arquillian/arquillian-selenium-bom/archive/master.zip"
                    home "mydirectory"
                    extractMapper {
                        toDir("${home}")
                        cutdirs()
                    }
                    postActions {
                        assertThat project, is(notNullValue())
                        assertThat project.spacelift, is(notNullValue())
                        assertThat project.spacelift.installationsDir, is(notNullValue())
                        assertThat "${home}".toString(), containsString("${project.spacelift.workspace}/mydirectory")
                        assertThat home.exists(), is(true)
                        assertThat new File(home, "pom.xml").exists(), is(true)
                    }
                }
                secondInstallation(inherits:firstInstallation) {
                    home "mydirectory2"
                    postActions {
                        assertThat "${home}".toString(), containsString("${project.spacelift.workspace}/mydirectory2")
                        assertThat home.exists(), is(true)
                        assertThat new File(home, "pom.xml").exists(), is(true)
                    }
                }
            }
        }

        project.spacelift.installations.each { installation ->
            installation.install(project.logger)
        }
    }

    @Test
    void "inherit test execution"() {
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'spacelift'

        project.spacelift {
            tests {

                def counter = 0
                def value = { if(counter==0) { counter++; return "foo"} else return "bar"}

                firstTest {
                    dataProvider { ["foo"]}
                    execute { data ->
                        assertThat data, is(value())
                    }
                }
                secondTest(inherits:"firstTest") { dataProvider { ["bar"] } }
            }
        }

        project.spacelift.tests.each { test ->
            test.executeTest(project.logger)
        }
    }

    @Test
    void "inherit test execution direct reference"() {
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'spacelift'

        project.spacelift {

            def counter = 0
            def value = { if(counter==0) { counter++; return "foo"} else return "bar"}

            tests {
                firstTest {
                    dataProvider { ["foo"]}
                    execute { data ->
                        assertThat data, is(value())
                    }
                }
                secondTest(inherits:firstTest) { dataProvider { ["bar"] }; beforeTest { data -> println data}}
            }
        }

        println "Total tests ${project.spacelift.tests.size()}"

        project.spacelift.tests.each { test ->
            test.executeTest(project.logger)
        }
    }

    @Test
    void "inherit profile definition"() {
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'spacelift'

        // enable second profile
        project.ext.set("profile2", "true")

        project.spacelift {
            profiles {
                profile1 { tests 'firstTest', 'secondTest', 'toBeIgnoredTest' }
                profile2(inherits:"profile1") { excludedTests 'toBeIgnoredTest' }
            }
            tests {
                firstTest {}
                secondTest {}
                toBeIgnoredTest {}
            }
        }

        project.tasks['init'].execute()

        assertThat project.selectedProfile, is(notNullValue())
        assertThat project.selectedProfile.name, is('profile2')
        assertThat project.selectedInstallations.size(), is(0)
        assertThat project.selectedTests.size(), is(2)
    }

    @Test
    void "inherit profile definition direct reference"() {
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'spacelift'

        // enable second profile
        project.ext.set("profile2", "true")

        project.spacelift {
            profiles {
                profile1 { tests 'firstTest', 'secondTest', 'toBeIgnoredTest' }
                profile2(inherits: profile1) { excludedTests 'toBeIgnoredTest' }
            }
            tests {
                firstTest {}
                secondTest {}
                toBeIgnoredTest {}
            }
        }

        project.tasks['init'].execute()

        assertThat project.selectedProfile, is(notNullValue())
        assertThat project.selectedProfile.name, is('profile2')
        assertThat project.selectedInstallations.size(), is(0)
        assertThat project.selectedTests.size(), is(2)
    }

    @Test
    void "inherit profile reference wrong type"() {
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'spacelift'

        // enable second profile
        project.ext.set("profile2", "true")

        exception.expect(MissingPropertyException)
        exception.expectMessage("tool1")

        project.spacelift {
            tools { tool1 { command "java" } }
            profiles {
                profile1 { tests 'firstTest', 'secondTest', 'toBeIgnoredTest' }
                profile2(inherits: tool1) { excludedTests 'toBeIgnoredTest' }
            }
            tests {
                firstTest {}
                secondTest {}
                toBeIgnoredTest {}
            }
        }

        project.tasks['init'].execute()

        assertThat project.selectedProfile, is(notNullValue())
        assertThat project.selectedProfile.name, is('profile2')
        assertThat project.selectedInstallations.size(), is(0)
        assertThat project.selectedTests.size(), is(2)
    }
}
