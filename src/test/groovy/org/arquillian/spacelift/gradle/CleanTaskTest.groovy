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
    public void cleanTest() {
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'spacelift'
        project.spacelift {
            profiles { 'default' { enabledInstallations 'someInstallation' } }
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

        project.getTasks()['clean:installations'].execute()
        assertThat project.spacelift.workspace.exists(), is(true)
        assertThat project.spacelift.installations['someInstallation'].home.exists(), is(false)

        project.getTasks()['clean:workspace'].execute()
        assertThat project.spacelift.workspace.exists(), is(false)
    }
}