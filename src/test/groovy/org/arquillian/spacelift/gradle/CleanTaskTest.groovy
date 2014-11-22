package org.arquillian.spacelift.gradle

import static org.junit.Assert.*
import static org.hamcrest.CoreMatchers.*

import org.junit.Ignore;
import org.junit.Test
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

import java.io.File

@Ignore("This test depends on installation which is not always guaranteed to exist.")
class CleanTaskTest {

    @Test
    public void cleanTest() {
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'spacelift'

        project.repositories { mavenCentral() }

        project.spacelift {

            workspace = new File(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString())
            installationsDir = new File(workspace, "installations")

            tests {
            }
            tools {
            }
            profiles {
                'default' {
                    enabledInstallations 'someInstallation'
                }
            }
            installations {
                someInstallation {
                    remoteUrl "https://github.com/smiklosovic/test/archive/master.zip"
                }
            }
        }

        GradleSpacelift.currentProject(project)

        project.getTasks()['init'].execute()
        project.getTasks()['prepare-env'].execute()
        
        assertThat project.spacelift.workspace.exists(), is(true)
        assertThat project.spacelift.installationsDir.exists(), is(true)

        project.getTasks()['cleanInstallations'].execute()
        assertThat project.spacelift.workspace.exists(), is(true)
        assertThat project.spacelift.installationsDir.exists(), is(false)
        
        project.getTasks()['cleanWorkspace'].execute()
        assertThat project.spacelift.workspace.exists(), is(false)
    }

}