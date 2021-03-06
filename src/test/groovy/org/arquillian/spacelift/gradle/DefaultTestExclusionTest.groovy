package org.arquillian.spacelift.gradle;

import static org.hamcrest.CoreMatchers.*
import static org.junit.Assert.assertThat

import org.junit.Test;
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

class DefaultTestExclusionTest {

    @Test
    void "excluded tests triggers"() {
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'org.arquillian.spacelift'

        project.spacelift {
            profiles {
                'default' {
                    tests 'bar', 'baz', 'foo'
                    excludedTests 'baz', 'foo'
                }
            }
            tests {
                bar { execute { println "Executing bar test" } }
                baz { execute { println "Executing baz test" } }
                foo { execute { println "Executing foo test" } }
            }
            tools {
            }
            installations {
            }
        }

        project.getTasks()['init'].execute()
        assertThat project.selectedProfile, is(notNullValue())
        assertThat project.selectedProfile.name, is('default')
        assertThat project.selectedTests.size(), is(1) // only 'bar' test is selected
    }
}
