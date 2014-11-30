package org.arquillian.spacelift.gradle

import static org.hamcrest.CoreMatchers.*
import static org.junit.Assert.assertThat

import org.arquillian.spacelift.execution.Tasks
import org.arquillian.spacelift.gradle.maven.SettingsXmlUpdater
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

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
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'spacelift'

        project.spacelift {
            tools {
                rhc { command "rhc" }
            }
            profiles {
                foobar { enabledInstallations ["eap"] }
            }
            installations {
                eap {
                }
            }
            tests {
            }
        }

        Tasks.prepare(SettingsXmlUpdater)
    }
}
