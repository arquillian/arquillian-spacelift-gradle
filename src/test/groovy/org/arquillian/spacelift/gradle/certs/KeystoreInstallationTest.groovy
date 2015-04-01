package org.arquillian.spacelift.gradle.certs

import org.arquillian.spacelift.gradle.Installation
import org.arquillian.spacelift.gradle.utils.EnvironmentUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Assume
import org.junit.Test

import static org.hamcrest.CoreMatchers.is
import static org.junit.Assert.assertThat

class KeystoreInstallationTest {

    @Test
    void "install keystore from embedded template"() {
        Assume.assumeThat EnvironmentUtils.runsOnLinux(), is(true)

        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'spacelift'

        project.spacelift {
            workspace = new File(System.getProperty("user.dir"), "workspace")
            tools {
                keytool {
                    command  "keytool"
                }

            }
            installations {
                defaultKeystore(from:KeystoreInstallation) {
                }
                // this certificate won't modify truststore hence no chance to create it by mistake
                defaultKeystoreWithNoGen(from:KeystoreInstallation) {
                    generateLocalCert false
                    home "othercerts"
                }
            }
        }

        project.spacelift.installations.each { Installation installation ->
            installation.install(project.logger)
            assertThat installation.isInstalled(), is(true)
        }
    }

}
