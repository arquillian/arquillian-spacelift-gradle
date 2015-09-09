package org.arquillian.spacelift.gradle.maven

import org.arquillian.spacelift.Spacelift
import org.arquillian.spacelift.gradle.SpaceliftPlugin
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Ignore
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
    @Ignore
    public void profilesWithoutTests() {
        Project testProject = ProjectBuilder.builder().build()

        testProject.apply plugin: 'org.arquillian.spacelift'

        testProject.spacelift {
            workspace Spacelift.configuration().workspace()
            tools {
                rhc { command "rhc" }
            }
            profiles {
                foobar { enabledInstallations ["maven"] }
            }
            installations {
                maven(from:MavenInstallation) {
                    alias "mymvn"
                }
            }
            tests {
            }
        }

        // apparently we have two different SpaceliftConfigurations here, leading to
        // file being at wrong location. This is related to the test fixture issue
        Spacelift.task('settings-mymvn').execute().await()
    }
}
