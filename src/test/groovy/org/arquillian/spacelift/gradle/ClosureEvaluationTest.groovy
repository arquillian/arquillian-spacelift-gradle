package org.arquillian.spacelift.gradle

import static org.hamcrest.CoreMatchers.*
import static org.junit.Assert.*

import org.arquillian.spacelift.gradle.android.AndroidSdkUpdater
import org.arquillian.spacelift.process.CommandBuilder
import org.arquillian.spacelift.process.impl.CommandTool
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException

class ClosureEvaluationTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    void "closure local bindings"() {

        def project = defineProject(
                installations:{
                    foo {
                        product "test"
                        version "1"
                        fileName "index.html"
                        autoExtract false
                        remoteUrl "http://www.google.com"
                        postActions {
                            assertThat project, is(notNullValue())
                            assertThat project.spacelift, is(notNullValue())
                            assertThat project.spacelift.installationsDir, is(notNullValue())
                            assertThat "${fsPath}".toString(), containsString("${project.spacelift.installationsDir}/test/1/index.html")
                            assertThat tool('java'), is(notNullValue())
                            assertThat tool(AndroidSdkUpdater), is(notNullValue())
                        }
                    }
                },
                tools:{
                    java {
                        command {
                            assertThat project, is(notNullValue())
                            assertThat project.spacelift, is(notNullValue())
                            return tool(CommandTool).command(new CommandBuilder("java"))
                        }
                    }
                    java2 {
                        command {
                            assertThat project, is(notNullValue())
                            assertThat project.rootDir, is(notNullValue())
                            return tool(CommandTool).command(new CommandBuilder("${project.rootDir}/java"))
                        }
                    }
                },
                tests:{
                    bar {
                        execute {
                            assertThat project, is(notNullValue())
                            assertThat project.spacelift, is(notNullValue())
                            assertThat project.spacelift.tools, is(notNullValue())
                            assertThat project.spacelift.installations, is(notNullValue())
                            project.spacelift.tools.each { tool ->
                                org.junit.Assert.assertThat tool.name, containsString("java")
                            }
                        }
                    }
                })

        project.spacelift.tests.each { test ->
            test.executeTest(project.logger)
        }

        def java2Tool = GradleSpacelift.tools("java2")
        assertThat java2Tool, is(notNullValue())
    }

    @Test
    void "closure extract mapper bindings"() {

        def project = defineProject(installations:{
            foo {
                product "test"
                version "1"
                fileName "arquillian-selenium-bom-master.zip"
                remoteUrl "https://github.com/arquillian/arquillian-selenium-bom/archive/master.zip"
                home {"arquillian-selenium-${project.propagatedProperty}" }
                extractMapper {
                    remap("arquillian-selenium-bom-master/*").with("arquillian-selenium-${project.propagatedProperty}/*")
                }
                postActions {
                    assertThat project, is(notNullValue())
                    assertThat project.spacelift, is(notNullValue())
                    assertThat project.spacelift.installationsDir, is(notNullValue())
                    assertThat "${home}".toString(), containsString("${project.spacelift.workspace}/arquillian-selenium-${project.propagatedProperty}")
                }
            }
        })
    }

    @Test
    void "indirect installation reference"() {
        def project = defineProject(installations:{
            fooReferenced {
                product "test"
                version "1"
            }
            fooIndirect {
                product "test"
                version "1"
                postActions {
                    assertThat project.spacelift.installations['fooReferenced'], is(notNullValue())
                }
            }
        })
    }

    @Test
    void "property syntax installation reference"() {

        exception.expect(MissingPropertyException)
        exception.expectMessage("fooReferenced")


        def project = defineProject(installations:{
            fooReferenced {
                product "test"
                version "1"
            }
            fooIndirect {
                product "test"
                version "1"
                postActions {
                    assertThat project.spacelift.installations.fooReferenced, is(notNullValue())
                }
            }
        })
    }

    @Test
    void "direct installation reference"() {
        def project = defineProject(installations:{
            fooReferenced {
                product "test"
                version "1"
            }
            fooIndirect {
                product "test"
                version "1"
                postActions {
                    assertThat fooReferenced.version, is(notNullValue())
                }
            }
        })
    }

    @Test
    void "ambiguous installation reference"() {
        def project = defineProject(profiles:{ fooReferenced {} },installations:{
            fooReferenced {
                product "test"
                version "1"
            }
            fooIndirect {
                product "test"
                version "1"
                postActions {
                    assertThat fooReferenced.version, is(notNullValue())
                    assertEquals(Installation.class, fooReferenced.class)
                }
            }
        })
    }


    private Project defineProject(Map closures) {

        def profilesClosure = closures.get("profiles", {})
        def installationsClosure = closures.get("installations", {})
        def toolsClosure = closures.get("tools", {})
        def testsClosure = closures.get("tests", {})

        Project project = ProjectBuilder.builder().build()

        project.ext.set("defaultPropagatedProperty", "10")

        project.apply plugin: 'spacelift'

        project.spacelift {
            profiles profilesClosure
            installations installationsClosure
            tools toolsClosure
            tests testsClosure
        }

        project.spacelift.installations.each { installation ->
            installation.install(project.logger)
        }

        project
    }
}
