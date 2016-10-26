package org.arquillian.spacelift.gradle;

import static org.hamcrest.CoreMatchers.*
import static org.junit.Assert.assertThat

import org.arquillian.spacelift.Spacelift
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.arquillian.spacelift.gradle.utils.KillTask
import org.junit.Test

public class KillTaskTest {

    @Test
    public void killTaskTest() {
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'org.arquillian.spacelift'

        project.spacelift {
            tools {
            }
            profiles {
            }
            installations {
            }
            tests {
            }
        }

        // kill servers
        Spacelift.task(KillTask).execute().await()
    }
}
