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

        project.apply plugin: 'org.arquillian.spacelift'
        project.spacelift {
            profiles {
                'default' {
                    enabledInstallations 'someInstallation'
                }
            }
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
        project.getTasks()['test'].execute()
        project.getTasks()['default'].execute()
        assertThat project.spacelift.workspace.exists(), is(true)
        assertThat project.spacelift.cacheDir.exists(), is(true)

        project.getTasks()['clean'].execute()
        assertThat project.spacelift.workspace.exists(), is(false)
    }

    @Test
    void "clean installation without home"() {
        Project testProject = ProjectBuilder.builder().build()

        testProject.apply plugin: 'org.arquillian.spacelift'
        testProject.spacelift {
            profiles {
                'default' {
                    enabledInstallations 'someInstallation'
                }
            }
            installations {
                someInstallation {
                    product 'test'
                    version '1'
                }
            }
        }

        // direct task execution does not support dependencies
        testProject.getTasks()['test'].execute()
        testProject.getTasks()['default'].execute()

        assertThat testProject.spacelift.workspace.exists(), is(true)

        testProject.getTasks()['clean'].execute()
        assertThat testProject.spacelift.workspace.exists(), is(false)
    }
}
