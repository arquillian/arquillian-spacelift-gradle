package org.arquillian.spacelift.gradle.configuration

import org.gradle.api.Project
import org.gradle.api.tasks.TaskExecutionException
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException

import static org.hamcrest.CoreMatchers.*
import static org.junit.Assert.assertThat

/**
 * Created by tkriz on 26/03/15.
 */
class ConfigurationItemParseTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    void "Basic property parsing"() {
        spacelift(item1: 'false') {
            configuration {
                item1 type: Boolean, {
                    type Boolean
                    defaultValue true
                }
                item2 {
                    type Class
                    defaultValue Boolean
                }
                item3 {
                    type String
                    value "Hello world!"
                }
            }
            profiles {
                'default' {
                    tests {
                        'test'
                    }
                }
            }
            tests {
                test {
                    execute {
                        assertThat item1, is(false)
                        assertThat item2, sameInstance(Boolean)
                        assertThat item3, is("Hello world!")
                    }
                }
            }
        }
    }

    @Test
    void "Profile property override parsing"() {
        spacelift(item1: 'false') {
            configuration {
                item1 type: Boolean, {
                    type Boolean
                    defaultValue true
                }
                item2 {
                    type Class
                    defaultValue Boolean
                }
                item3 {
                    type String
                    value "Hello world!"
                }
            }
            profiles {
                'default' {
                    configuration {
                        item3 {
                            type String
                            value "World!"
                        }
                    }
                    tests {
                        'test'
                    }
                }
            }
            tests {
                test {
                    execute {
                        assertThat item1, is(false)
                        assertThat item2, sameInstance(Boolean)
                        assertThat item3, is("World!")
                    }
                }
            }
        }
    }

    @Test
    void "Profile property override with wrong type"() {
        exception.expect(TaskExecutionException)
        exception.expectCause(isA(IllegalConfigurationException))

        spacelift(item1: 'false') {
            configuration {
                item1 type: Boolean, {
                    type Boolean
                    defaultValue true
                }
                item2 {
                    type Class
                    defaultValue Boolean
                }
                item3 {
                    type String
                    value "Hello world!"
                }
            }
            profiles {
                'default' {
                    configuration {
                        item3 {
                            type Boolean
                            value true
                        }
                    }
                    tests {
                        'test'
                    }
                }
            }
            tests {
                test {
                    execute {
                        assertThat item1, is(false)
                        assertThat item2, sameInstance(Boolean)
                        assertThat item3, is("World!")
                    }
                }
            }
        }
    }

    static void spacelift(Map<String, Object> properties = [:], Closure buildScript) {
        Project project = ProjectBuilder.builder().build()

        properties.each { key, value ->
            project.extensions.add(key, value)
        }

        project.apply(plugin: 'spacelift')

        project.spacelift buildScript
        project.getTasks()['describe'].execute()
        project.getTasks()['init'].execute()
        project.getTasks()['test'].execute()
    }
}
