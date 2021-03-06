package org.arquillian.spacelift.gradle

import static org.hamcrest.CoreMatchers.*
import static org.junit.Assert.assertThat

import org.gradle.api.Project
import org.arquillian.spacelift.gradle.configuration.BuiltinConfigurationItemConverters
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException

import static org.junit.Assert.fail

class DefaultTestExecutionLifecycleTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    void "all phases should execute twice when dataProvider returns an array with two items"() {
        runAsSpaceliftTest {

            def counter = 0
            def value = { if(counter<3) { counter++; return "first" } else return "second" }

            dataProvider { ["first", "second"] }
            beforeSuite { }
            beforeTest { data ->
                assertThat data, is(value())
            }
            execute { data ->
                assertThat data, is(value())
            }
            afterTest { data ->
                assertThat data, is(value())
            }
            afterSuite { }
        }
    }

    @Test
    void "data provider as String[]"() {
        runAsSpaceliftTest {

            def counter = 0
            def value = { if(counter<3) { counter++; return "first" } else return "second" }

            dataProvider { "first,second".split(",") }
            beforeSuite { }
            beforeTest { data ->
                assertThat data, is(value())
            }
            execute { data ->
                assertThat data, is(value())
            }
            afterTest { data ->
                assertThat data, is(value())
            }
            afterSuite { }
        }
    }

    @Test
    void "dataProvider with ext: ext before spacelift is applied"() {
        Project project = ProjectBuilder.builder().build()

        project.ext.array = '["second", "first"]'


        project.apply plugin: 'org.arquillian.spacelift'

        project.spacelift {
            configuration {
                array {
                    defaultValue { ["first", "second"] }
                    converter BuiltinConfigurationItemConverters.getConverter(String[])
                }
            }
            profiles {
                "default" {
                    tests "foo"
                }
            }
            tests {
                foo {
                    def counter = 0
                    def value = { if(counter<3) { counter++; return "second" } else return "first" }

                    dataProvider {
                        println array
                        return array
                    }
                    beforeSuite { }
                    beforeTest { data ->
                        assertThat data, is(value())
                    }
                    execute { data ->
                        assertThat data, is(value())
                    }
                    afterTest { data ->
                        assertThat data, is(value())
                    }
                    afterSuite { }
                }
            }
        }

        project.tasks['init'].execute()
        project.spacelift.tests.each { test ->
            test.executeTest(project.logger)
        }
    }

    @Test
    void "dataProvider with ext: ext after spacelift is applied"() {
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'org.arquillian.spacelift'

        project.ext {
            array = '["first", "second"]'
        }

        project.spacelift {
            configuration {
                array {
                    defaultValue { ["first", "second"] }
                    converter BuiltinConfigurationItemConverters.getConverter(String[])
                }
            }
            profiles {
                "default" {
                    tests '*'
                }
            }
            tests {
                foo {
                    def counter = 0
                    def value = { if(counter<3) { counter++; return "first" } else return "second" }

                    dataProvider { array }
                    beforeSuite { }
                    beforeTest { data ->
                        assertThat data, is(value())
                    }
                    execute { data ->
                        assertThat data, is(value())
                    }
                    afterTest { data ->
                        assertThat data, is(value())
                    }
                    afterSuite { }
                }
            }
        }

        project.tasks['init'].execute()
        project.spacelift.tests.each { test ->
            test.executeTest(project.logger)
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
    void "original exception should be propagated"() {

        exception.expect(IllegalStateException)
        exception.expectMessage("Random test error.")

        runAsSpaceliftTest {
            execute {
                throw new IllegalStateException("Random test error.")
            }
            afterTest {
                throw new IllegalArgumentException("Random test error.")
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

        project.apply plugin: 'org.arquillian.spacelift'

        project.spacelift buildScript

        project.ext.exception = exception

        project.spacelift.tests.each { test ->
            test.executeTest(project.logger)
        }
    }
}
