package org.arquillian.spacelift.gradle

import static org.hamcrest.CoreMatchers.*
import static org.junit.Assert.assertThat

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule;
import org.junit.Test
import org.junit.rules.ExpectedException;

import org.arquillian.spacelift.gradle.SpaceliftPlugin

class InheritanceTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    void "inherit installation properties"() {
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'org.arquillian.spacelift'

        project.spacelift {
            workspace { new File(System.getProperty("user.dir"), "workspace") }
            installations {
                firstInstallation {
                    product "selenium"
                    version "whatever"
                    remoteUrl "https://github.com/arquillian/arquillian-selenium-bom/archive/master.zip"
                    home "mydirectory"
                    extractMapper {
                        toDir("${home}")
                        cutdirs()
                    }
                    postActions {
                        assertThat project, is(notNullValue())
                        assertThat project.spacelift, is(notNullValue())
                        assertThat project.spacelift.cacheDir, is(notNullValue())
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
            SpaceliftPlugin.installInstallation(installation, project.logger)
        }
    }

    @Test
    void "inherit installation direct reference"() {
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'org.arquillian.spacelift'
        project.spacelift {
            workspace { new File(System.getProperty("user.dir"), "workspace") }
            installations {
                firstInstallation {
                    product "seleniumbom"
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
                        assertThat project.spacelift.cacheDir, is(notNullValue())
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
            SpaceliftPlugin.installInstallation(installation, project.logger)
        }
    }

    @Test
    void "inherit test execution"() {
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'org.arquillian.spacelift'

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
    void "class inheritance instruments superclass' fields"() {
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'org.arquillian.spacelift'

        // enable second profile
        project.ext.set("profile1", "true")

        project.spacelift {
            profiles {
                profile1 { tests 'test1' }
            }
            tests {
                test1(from: MyOwnDefaultTestChild) {
                    randomString { 'Test1' }

                }
            }
        }

        project.tasks['assemble'].execute()
        project.tasks['profile1'].execute()

        assertThat project.selectedProfile, is(notNullValue())
        assertThat project.selectedProfile.name, is('profile1')
        assertThat project.selectedInstallations.size(), is(0)
        assertThat project.selectedTests.size(), is(1)

        assertThat project.spacelift.test1.randomString.resolve(), is('Test1')
    }

    @Test
    void "inherit test execution direct reference"() {
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'org.arquillian.spacelift'

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

        project.apply plugin: 'org.arquillian.spacelift'

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

        project.tasks['assemble'].execute()
        project.tasks['profile2'].execute()

        assertThat project.selectedProfile, is(notNullValue())
        assertThat project.selectedProfile.name, is('profile2')
        assertThat project.selectedInstallations.size(), is(0)
        assertThat project.selectedTests.size(), is(2)
    }

    @Test
    void "inherit profile definition direct reference"() {
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'org.arquillian.spacelift'



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

        project.tasks['assemble'].execute()
        project.tasks['profile2'].execute()

        assertThat project.selectedProfile, is(notNullValue())
        assertThat project.selectedProfile.name, is('profile2')
        assertThat project.selectedInstallations.size(), is(0)
        assertThat project.selectedTests.size(), is(2)
    }

    @Test
    void "inherit profile reference wrong type"() {
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'org.arquillian.spacelift'

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

        project.tasks['assemble'].execute()
        project.tasks['profile2'].execute()

        assertThat project.selectedProfile, is(notNullValue())
        assertThat project.selectedProfile.name, is('profile2')
        assertThat project.selectedInstallations.size(), is(0)
        assertThat project.selectedTests.size(), is(2)
    }

}
