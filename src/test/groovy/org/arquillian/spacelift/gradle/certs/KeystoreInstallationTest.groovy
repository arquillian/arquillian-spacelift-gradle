package org.arquillian.spacelift.gradle.certs

import org.arquillian.spacelift.gradle.utils.EnvironmentUtils
import org.junit.Assume

import static org.hamcrest.CoreMatchers.*
import static org.junit.Assert.assertThat

import org.arquillian.spacelift.execution.ExecutionException;
import org.arquillian.spacelift.gradle.Installation
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException

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

        // on purpose, we are not installing here as this installation will download zillion of data
        // from internet, just verify that previous manual definition installed the SDK and tools are
        // properly registered
        project.spacelift.installations.each { Installation installation ->
            installation.install(project.logger)
            assertThat installation.isInstalled(), is(true)
        }
    }

}
