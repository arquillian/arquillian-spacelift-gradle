package org.arquillian.spacelift.gradle

import static org.hamcrest.CoreMatchers.*
import static org.junit.Assert.assertThat

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

import org.arquillian.spacelift.gradle.SpaceliftPlugin

/**
 * Asserts that installations can be specified without home
 * @author <a href="mailto:kpiwko@redhat.com">Karel Piwko</a>
 *
 */
public class InstallationParsingTest {

    @Test
    public void noHomeInstallation() {
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'org.arquillian.spacelift'

        project.spacelift {
            tools {
                rhc {
                    command "rhc"
                }
            }
            profiles {
            }
            installations { eap { } }
            tests {
            }
        }

        project.spacelift.installations.each { installation ->
            assertThat installation.home, is(notNullValue())
        }
    }

    @Test
    public void preconditionTest() {

        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'org.arquillian.spacelift'

        project.spacelift {
            tools {
            }
            profiles {
            }
            tests {
            }
            installations {
                didNotMeetPreconditionInstallation {
                    preconditions { false }
                    postActions {
                        new File(System.getProperty("java.io.tmpdir"), "preconditionTestFile.tmp").createNewFile()
                    }
                }
            }
        }

        project.spacelift.installations.each { installation ->
            SpaceliftPlugin.installInstallation(installation, project.logger)
        }

        assertThat new File(System.getProperty("java.io.tmpdir"), "preconditionTestFile.tmp").exists(), is(false)
    }
    
    @Test
    public void extractMapperCutsdirTest() {
        Project testProject = ProjectBuilder.builder().build()

        def project = testProject
        testProject.apply plugin: 'org.arquillian.spacelift'

        testProject.spacelift {
            workspace { new File(System.getProperty("user.dir"), "workspace") }
            installations {
                foo {
                    product "selenium-bom"
                    version "whatever"
                    remoteUrl "https://github.com/arquillian/arquillian-selenium-bom/archive/master.zip"
                    home "mydirectory"
                    extractMapper {
                        toDir("${home}")
                        cutdirs()
                    }
                    postActions {
                        org.junit.Assert.assertThat project, is(notNullValue())
                        org.junit.Assert.assertThat project.spacelift, is(notNullValue())
                        org.junit.Assert.assertThat project.spacelift.cacheDir, is(notNullValue())
                        org.junit.Assert.assertThat "${home}".toString(), containsString("${project.spacelift.workspace}/mydirectory")                        
                        org.junit.Assert.assertThat home.exists(), is(true)
                        org.junit.Assert.assertThat new File(home, "pom.xml").exists(), is(true)
                    }
                }
            }
        }

        testProject.spacelift.installations.each { installation ->
            SpaceliftPlugin.installInstallation(installation, project.logger)
        }
    }
}
