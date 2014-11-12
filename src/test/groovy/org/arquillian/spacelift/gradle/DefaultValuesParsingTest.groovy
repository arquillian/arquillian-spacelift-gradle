package org.arquillian.spacelift.gradle

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

import static org.hamcrest.CoreMatchers.*
import static org.junit.Assert.assertThat

/**
 * Ensures that setting default value reflect to values available in project
 * @author <a href="mailto:kpiwko@redhat.com">Karel Piwko</a>
 *
 */
class DefaultValuesParsingTest {

    @Test
    public void parseDefaultValues() {

        Project project = ProjectBuilder.builder().build()

        project.ext.set("defaultAndroidTargets", ["19", "18", "google-17"])

        project.apply plugin: 'spacelift'

        // find android targets from default value
        def androidTargets = project.androidTargets
        assertThat androidTargets, is(notNullValue())
        assertThat androidTargets[0], is("19")
    }

    @Test
    public void parseDefaultValue() {

        Project project = ProjectBuilder.builder().build()

        project.ext.set("defaultMyVersion", "1.2.3")

        project.apply plugin: 'spacelift'

        // find default value if not an array
        def myVersion = project.myVersion
        assertThat myVersion, is(notNullValue())
        assertThat myVersion, is("1.2.3")
    }

    @Test
    public void useDefaultValuesInDSL() {

        Project project = ProjectBuilder.builder().build()

        project.ext.set("defaultRhcVersion", "10")

        project.apply plugin: 'spacelift'

        project.spacelift {
            tools {
                rhc { command "rhc${project.rhcVersion}" }
            }
            profiles {
            }
        }
        
        // initialize current project tools - this is effectively init-tools task
        GradleSpacelift.currentProject(project)

        // find rhc tool
        def rhcTool = GradleSpacelift.tools("rhc")
        assertThat rhcTool, is(notNullValue())

        // ensure it is pointing to the right binary
        assertThat rhcTool.toString(), containsString("rhc10")
    }

    @Test
    public void overrideDefaultValues() {

        Project project = ProjectBuilder.builder().build()

        project.ext.set("defaultAndroidTargets", ["19", "18", "google-17"])
        project.ext.androidTargets = "18, 19"

        project.apply plugin: 'spacelift'

        // find android targets from default value
        def androidTargets = project.androidTargets
        assertThat androidTargets, is(notNullValue())
        assertThat androidTargets[0], is("18")
    }
}
