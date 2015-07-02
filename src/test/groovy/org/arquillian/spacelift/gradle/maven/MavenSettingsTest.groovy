package org.arquillian.spacelift.gradle.maven

import org.arquillian.spacelift.Spacelift
import org.arquillian.spacelift.gradle.SpaceliftPlugin
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

import static org.hamcrest.CoreMatchers.containsString
import static org.hamcrest.CoreMatchers.is
import static org.junit.Assert.assertThat

class MavenSettingsTest {

    @Test
    public void loadBackupSettingsXml() {
        File file = File.createTempFile("settings.xml", "-spacelift");
        this.getClass().getResource("/settings.xml-template").withInputStream { ris ->
            file.withOutputStream { fos -> fos << ris }
        }
        assertThat file.exists(), is(true)
        assertThat file.text, containsString("settings")
    }
    
    @Test
    public void profilesWithoutTests() {
        Project testProject = ProjectBuilder.builder().build()

        testProject.apply plugin: 'org.arquillian.spacelift'

        testProject.spacelift {
            tools {
                rhc { command "rhc" }
            }
            profiles {
                foobar { enabledInstallations ["eap"] }
            }
            installations {
                maven(from:MavenInstallation) {
                    alias "mymvn"
                }
            }
            tests {
            }
        }

        testProject.spacelift.installations.each { installation ->
            SpaceliftPlugin.installInstallation(installation, testProject.logger)
        }

        Spacelift.task('settings-mymvn').execute().await()
    }
}
