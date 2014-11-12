package org.arquillian.spacelift.gradle;

import static org.junit.Assert.*;

import org.junit.Test;
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

class TestExecutionLifecycleTest {

    @Test
    public void dataProviderTest() {
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'spacelift'

        project.spacelift {
            tests {
                bar {
                    dataProvider { ["first", "second"] }
                    beforeSuite { println "Executing beforeSuite" }
                    beforeTest { value -> println "Executing beforeTest with data ${value}" }
                    execute { value -> println "Executing test with data ${value}" }
                    afterTest { value -> println "Executing afterTest with data ${value}" }
                    afterSuite { println "Executing afterSuite" }
                }
            }
            tools {
            }
            profiles {
            }
            installations {
            }
        }

        GradleSpacelift.currentProject(project)

        project.spacelift.tests.each { test ->
            test.executeTest(project.logger)
        }
    }

    @Test
    public void noDataProviderTest() {
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'spacelift'

        project.spacelift {
            tests {
                bar {
                    beforeSuite { println "Executing beforeSuite" }
                    beforeTest {  println "Executing beforeTest" }
                    execute {  println "Executing test" }
                    afterTest {  println "Executing afterTest" }
                    afterSuite { println "Executing afterSuite" }
                }
            }
            tools {
            }
            profiles {
            }
            installations {
            }
        }

        GradleSpacelift.currentProject(project)

        project.spacelift.tests.each { test ->
            test.executeTest(project.logger)
        }
    }
}
