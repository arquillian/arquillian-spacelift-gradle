package org.arquillian.spacelift.gradle

import static org.junit.Assert.*
import static org.hamcrest.CoreMatchers.*

import org.junit.Ignore;
import org.junit.Test
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

import java.io.File


class CleanTaskTest {

    @Test
    void "clean installations and workspace"() {
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'spacelift'
        project.spacelift {
            profiles { 'default' { enabledInstallations 'someInstallation'
                } }
            installations {
                someInstallation {
                    product 'test'
                    version '1'
                    remoteUrl "https://github.com/smiklosovic/test/archive/master.zip"
                    home "foobar"
                    extractMapper { toDir(home).cutdirs() }
                }
            }
        }

        // direct task execution does not support dependencies
        project.getTasks()['init'].execute()
        project.getTasks()['assemble'].execute()

        assertThat project.spacelift.workspace.exists(), is(true)
        assertThat project.spacelift.installationsDir.exists(), is(true)

        project.getTasks()['cleanInstallations'].execute()
        assertThat project.spacelift.workspace.exists(), is(true)
        assertThat project.spacelift.installations['someInstallation'].home.exists(), is(false)

        project.getTasks()['cleanWorkspace'].execute()
        assertThat project.spacelift.workspace.exists(), is(false)
    }

    @Test
    void "clean installation without home"() {
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'spacelift'
        project.spacelift {
            profiles { 'default' { enabledInstallations 'someInstallation'
                } }
            installations {
                someInstallation {
                    product 'test'
                    version '1'
                }
            }
        }

        // direct task execution does not support dependencies
        project.getTasks()['init'].execute()
        project.getTasks()['assemble'].execute()

        assertThat project.spacelift.workspace.exists(), is(true)        

        project.getTasks()['cleanInstallations'].execute()
        assertThat project.spacelift.workspace.exists(), is(true)
        assertThat "Installation home dir was not deleted as it equals workspace", project.spacelift.installations['someInstallation'].home.exists(), is(true)

        project.getTasks()['cleanWorkspace'].execute()
        assertThat project.spacelift.workspace.exists(), is(false)
    }
}