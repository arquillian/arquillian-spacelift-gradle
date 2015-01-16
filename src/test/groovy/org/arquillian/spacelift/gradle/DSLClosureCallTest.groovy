package org.arquillian.spacelift.gradle

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule;
import org.junit.Test
import org.junit.rules.ExpectedException;

class DSLClosureCallTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    void "inherit local methods from closure is not allowed"() {
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'spacelift'

        project.ext.exception = exception
        // enable first profile
        project.ext.set("profile1", "true")

        project.spacelift {
            tests {
                firstTest {
                    project.exception.expect(GroovyRuntimeException)
                    // here you can't define sharedAsFoo
                    sharedAsFoo {x, y -> println x, y}
                }
                secondTest(from:firstTest) {
                    sharedAsFoo("I'm in secondTest", "a")
                }
            }
        }
    }
}
