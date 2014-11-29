package org.arquillian.spacelift.gradle

import static org.hamcrest.CoreMatchers.*
import static org.junit.Assert.assertThat

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

class InheritanceTest {

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

        GradleSpacelift.currentProject(project)

        project.spacelift.installations.each { installation ->
            installation.install(project.logger)
        }
    }

    @Test
    void "inherit test execution"() {
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'spacelift'

        project.ext.verifier = [:]
        project.verifier['invoked']=0
        project.verifier['0']='foo'
        project.verifier['1']='bar'

        project.spacelift {
            tests {
                firstTest {
                    dataProvider { ["foo"]}
                    execute { foo ->
                        println foo
                        assertThat foo, is(project.verifier["${project.verifier.invoked}"])
                        project.verifier.invoked += 1
                    }
                }
                secondTest(inherits:"firstTest") { dataProvider { ["bar"]} }
            }
        }

        GradleSpacelift.currentProject(project)

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

        GradleSpacelift.currentProject(project)
        project.tasks['init'].execute()

        assertThat project.selectedProfile, is(notNullValue())
        assertThat project.selectedProfile.name, is('profile2')
        assertThat project.selectedInstallations.size(), is(0)
        assertThat project.selectedTests.size(), is(2)
    }
}
