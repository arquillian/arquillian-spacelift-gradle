package org.arquillian.spacelift.gradle.git

import static org.hamcrest.CoreMatchers.*
import static org.junit.Assert.assertThat

import org.arquillian.spacelift.Spacelift
import org.arquillian.spacelift.gradle.Installation
import org.arquillian.spacelift.gradle.android.AndroidSdkInstallation
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

class GitBasedInstallationTest {

    @Test
    void "install from commit and from master"() {
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'spacelift'

        project.spacelift {
            workspace = new File(System.getProperty("user.dir"), "workspace")
            installations {
                theCommit(from:GitBasedInstallation) {
                    repository "ssh://git@github.com/smiklosovic/test.git"
                    commit '15b1d748935a58ea0f583a4d21531e854e6c382a'
                    home "smikloso-test"
                }
                theMaster(from:GitBasedInstallation) {
                    repository "ssh://git@github.com/smiklosovic/test.git"
                    commit 'master'
                    home "smikloso-test"
                }
                theCommitAgain(from:GitBasedInstallation) {
                    repository "ssh://git@github.com/smiklosovic/test.git"
                    commit '15b1d748935a58ea0f583a4d21531e854e6c382a'
                    home "smikloso-test"
                }
                // we need to end up with different commit than we've started
                theMasterAgain(from:GitBasedInstallation) {
                    repository "ssh://git@github.com/smiklosovic/test.git"
                    commit 'master'
                    home "smikloso-test"
                }
            }
        }

        // on purpose, we are not installing here as this installation will download zillion of data
        // from internet, just verify that previous manual definition installed the SDK and tools are
        // properly registered
        project.spacelift.installations.each { Installation installation ->
            assertThat installation.isInstalled(), is(false)
            installation.install(project.logger)
            assertThat installation.isInstalled(), is(true)
        }
    }

}
