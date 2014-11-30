package org.arquillian.spacelift.gradle

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException

import static org.junit.Assert.fail

class TestExecutionLifecycleTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    void "all phases should execute twice when dataProvider returns an array with two items"() {
        runAsSpaceliftTest {
            dataProvider { ["first", "second"] }
            beforeSuite { println "Executing beforeSuite" }
            beforeTest { value -> println "Executing beforeTest with data ${value}" }
            execute { value -> println "Executing test with data ${value}" }
            afterTest { value -> println "Executing afterTest with data ${value}" }
            afterSuite { println "Executing afterSuite" }
        }
    }

    @Test
    void "all phases should execute once when no dataProvider is specified"() {
        runAsSpaceliftTest {
            beforeSuite { println "Executing beforeSuite" }
            beforeTest { println "Executing beforeTest" }
            execute { println "Executing test" }
            afterTest { println "Executing afterTest" }
            afterSuite { println "Executing afterSuite" }
        }
    }

    @Test
    void "afterTest should be invoked when exception occurs in test"() {
        runAsSpaceliftTest {
            execute {
                throw new IllegalStateException("Random test error.")
            }
            afterTest {
                project.exception.expect(IllegalStateException)
                project.exception.expectMessage("Random test error.")
            }
        }
    }

    @Test
    void "afterSuite should be invoked when exception occurs in dataProvider"() {
        runAsSpaceliftTest {
            dataProvider {
                throw new IllegalStateException("Random test error.")
            }
            afterSuite {
                project.exception.expect(IllegalStateException)
                project.exception.expectMessage("Random test error.")
            }
        }
    }

    @Test
    void "afterSuite should be invoked when exception occurs in beforeTest"() {
        runAsSpaceliftTest {
            beforeTest {
                throw new IllegalStateException("Random test error.")
            }
            afterSuite {
                project.exception.expect(IllegalStateException)
                project.exception.expectMessage("Random test error.")
            }
        }
    }

    @Test
    void "afterSuite should be invoked when exception occurs in test"() {
        runAsSpaceliftTest {
            execute {
                throw new IllegalStateException("Random test error.")
            }
            afterSuite {
                project.exception.expect(IllegalStateException)
                project.exception.expectMessage("Random test error.")
            }
        }
    }

    @Test
    void "afterSuite should be invoked when exception occurs in afterTest"() {
        runAsSpaceliftTest {
            afterTest {
                throw new IllegalStateException("Random test error.")
            }
            afterSuite {
                project.exception.expect(IllegalStateException)
                project.exception.expectMessage("Random test error.")
            }
        }
    }

    @Test
    void "test and afterSuite should not execute when beforeSuite fails"() {
        runAsSpaceliftTest {
            beforeSuite {
                project.exception.expect(IllegalStateException)
                throw new IllegalStateException("Random before suite error.")
            }
            execute {
                fail("Test should not be run.")
            }
            afterSuite {
                fail("AfterSuite should not be run.")
            }
        }
    }

    @Test
    void "test and afterTest should not execute when beforeTest fails"() {
        runAsSpaceliftTest {
            beforeTest {
                project.exception.expect(IllegalStateException)
                throw new IllegalStateException("Random before suite error.")
            }
            execute {
                fail("Test should not be run.")
            }
            afterTest {
                fail("AfterTest should not be run.")
            }
        }
    }

    void runAsSpaceliftTest(Closure test) {
        runAsSpaceliftScript {
            tests {
                foo test
            }
        }
    }

    void runAsSpaceliftScript(Closure buildScript) {
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'spacelift'

        project.spacelift buildScript

        project.ext.exception = exception

        project.spacelift.tests.each { test ->
            test.executeTest(project.logger)
        }
    }
}
