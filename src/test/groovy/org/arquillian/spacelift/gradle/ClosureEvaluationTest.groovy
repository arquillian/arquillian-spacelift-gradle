package org.arquillian.spacelift.gradle

import static org.hamcrest.CoreMatchers.*
import static org.junit.Assert.*

import org.arquillian.spacelift.gradle.android.AndroidSdkUpdater
import org.arquillian.spacelift.process.CommandBuilder
import org.arquillian.spacelift.process.impl.CommandTool
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

class ClosureEvaluationTest {

    @Test
    public void closureDelegateBindingsTest() {
        Project testProject = ProjectBuilder.builder().build()
        def project = testProject
        testProject.apply plugin: 'spacelift'

        testProject.spacelift {
            installations {
                foo {
                    product "test"
                    version "1"
                    fileName "index.html"
                    autoExtract false
                    remoteUrl "http://www.google.com"
                    postActions {
                        org.junit.Assert.assertThat project, is(notNullValue())
                        org.junit.Assert.assertThat project.spacelift, is(notNullValue())
                        org.junit.Assert.assertThat project.spacelift.installationsDir, is(notNullValue())
                        org.junit.Assert.assertThat "${fsPath}".toString(), containsString("${project.spacelift.installationsDir}/test/1/index.html")
                        org.junit.Assert.assertThat tool('java'), is(notNullValue())
                        org.junit.Assert.assertThat tool(AndroidSdkUpdater), is(notNullValue())
                    }
                }
            }
            tools {
                java {
                    command {
                        org.junit.Assert.assertThat project, is(notNullValue())
                        org.junit.Assert.assertThat project.spacelift, is(notNullValue())
                        return tool(CommandTool).command(new CommandBuilder("java"))
                    }
                }
                java2 {
                    command {
                        org.junit.Assert.assertThat project, is(notNullValue())
                        org.junit.Assert.assertThat project.rootDir, is(notNullValue())
                        return tool(CommandTool).command(new CommandBuilder("${project.rootDir}/java"))
                    }
                }
            }
            tests {
                bar {
                    execute {
                        org.junit.Assert.assertThat project, is(notNullValue())
                        org.junit.Assert.assertThat project.spacelift, is(notNullValue())
                        org.junit.Assert.assertThat project.spacelift.tools, is(notNullValue())
                        org.junit.Assert.assertThat project.spacelift.installations, is(notNullValue())
                        project.spacelift.tools.each { tool ->
                            org.junit.Assert.assertThat tool.name, containsString("java")
                        }
                    }
                }
            }
        }

        testProject.spacelift.installations.each { installation ->
            installation.install(project.logger)
        }

        testProject.spacelift.tests.each { test ->
            test.executeTest(project.logger)
        }

        def java2Tool = GradleSpacelift.tools("java2")
        assertThat java2Tool, is(notNullValue())
    }

    @Test
    public void extractMapperDelegateBindingsTest() {
        Project testProject = ProjectBuilder.builder().build()

        testProject.ext.set("defaultPropagatedProperty", "10")

        def project = testProject
        testProject.apply plugin: 'spacelift'

        testProject.spacelift {
            installations {
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
                        org.junit.Assert.assertThat project, is(notNullValue())
                        org.junit.Assert.assertThat project.spacelift, is(notNullValue())
                        org.junit.Assert.assertThat project.spacelift.installationsDir, is(notNullValue())
                        org.junit.Assert.assertThat "${home}".toString(), containsString("${project.spacelift.workspace}/arquillian-selenium-${project.propagatedProperty}")
                    }
                }
            }
        }

        testProject.spacelift.installations.each { installation ->
            installation.install(project.logger)
        }
    }
}
