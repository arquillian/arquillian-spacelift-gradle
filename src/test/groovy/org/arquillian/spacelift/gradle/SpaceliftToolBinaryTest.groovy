package org.arquillian.spacelift.gradle

import org.arquillian.spacelift.process.CommandBuilder
import org.arquillian.spacelift.process.ProcessResult
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Assume;
import org.junit.Test
import org.junit.Assert
import org.arquillian.spacelift.execution.Tasks
import org.arquillian.spacelift.process.impl.CommandTool

import static org.hamcrest.CoreMatchers.*
import static org.junit.Assert.assertThat


// NOTE some of these tests might not work in IDE
class SpaceliftToolBinaryTest {

    @Test
    public void addEnvironmentPropertyToTool() {

        Assume.assumeThat System.getenv("ANT_HOME"), is(notNullValue())

        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'spacelift'

        project.spacelift {
            tools {
                ant {
                    environment {
                        [ANT_HOME: System.getenv("ANT_HOME")]
                    }
                    command {"ant"}
                }
            }
            profiles {
            }
            installations {
            }
            tests {
            }
        }

        // find ant tool
        def antTool = GradleSpacelift.tools("ant")
        assertThat antTool, is(notNullValue())

        // call ant help
        GradleSpacelift.tools("ant").parameters("-help").interaction(GradleSpacelift.ECHO_OUTPUT).execute().await()
    }

    @Test
    void "env property as GStringImpl"() {

        Assume.assumeThat System.getenv("ANT_HOME"), is(notNullValue())

        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'spacelift'

        project.spacelift {
            tools {
                ant {
                    environment {
                        def foo = "foo"
                        [FOO_TEST: "${foo}"]
                    }
                    command {"ant"}
                }
            }
            profiles {
            }
            installations {
            }
            tests {
            }
        }

        // find ant tool
        def antTool = GradleSpacelift.tools("ant")
        assertThat antTool, is(notNullValue())

        // call ant help
        GradleSpacelift.tools("ant").parameters("-help").execute().await()
    }

    @Test
    public void multipleTools() {
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'spacelift'

        project.spacelift {
            tools {
                ant { command { new CommandBuilder("ant") }}
                mvn { command { new CommandBuilder("mvn") }}
            }
            profiles {
            }
            installations {
            }
            tests {
            }
        }

        // find ant tool
        def antTool = GradleSpacelift.tools("ant")
        assertThat antTool, is(notNullValue())

        // call ant help
        GradleSpacelift.tools("ant").parameters("-help").execute().await()

        // find mvn tool
        def mvnTool = GradleSpacelift.tools("mvn")
        assertThat mvnTool, is(notNullValue())

        // call mvn help
        GradleSpacelift.tools("mvn").parameters("-help").execute().await()

    }

    @Test
    public void binaryAsString() {
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'spacelift'

        project.spacelift {
            tools { ant { command "ant" } }
            profiles {
            }
            installations {
            }
            tests {
            }
        }

        // find ant tool
        def antTool = GradleSpacelift.tools("ant")
        assertThat antTool, is(notNullValue())

        // call ant help
        GradleSpacelift.tools("ant").parameters("-help").execute().await()
    }

    @Test
    public void binaryAsMap() {
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'spacelift'

        project.spacelift {
            tools {
                ant {
                    command ([linux:"ant", windows:"ant.bat"])
                }
            }
            profiles {
            }
            installations {
            }
            tests {
            }
        }

        // find ant tool
        def antTool = GradleSpacelift.tools("ant")
        assertThat antTool, is(notNullValue())

        // call ant help
        GradleSpacelift.tools("ant").parameters("-help").execute().await()
    }

    @Test
    public void binaryAsClosure() {
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'spacelift'

        project.spacelift {
            tools {
                ant {
                    command {
                        def antHome = System.getenv("ANT_HOME")
                        if (antHome != null && !antHome.isEmpty()) {
                            return new CommandBuilder(antHome + "/bin/ant")
                        } else {
                            return new CommandBuilder("ant")
                        }
                    }
                }
            }
            profiles {
            }
            installations {
            }
            tests {
            }
        }

        // find ant tool
        def antTool = GradleSpacelift.tools("ant")
        assertThat antTool, is(notNullValue())

        // call ant help
        ProcessResult result = GradleSpacelift.tools("ant").parameters("-help").execute().await()
        assertThat result.exitValue(), is(0)
    }

    @Test
    public void binaryAsMapOfClosures() {
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'spacelift'

        project.ext.set("androidHome", "foobar")

        project.spacelift {
            tools {
                android {
                    command ([
                        linux: {
                            new CommandBuilder(new File(project.androidHome, "tools/android.bat").getAbsolutePath())
                        },
                        windows: {
                            new CommandBuilder("cmd.exe", "/C", new File(project.androidHome, "tools/android.bat").getAbsolutePath())
                        }
                    ])
                }
            }
            profiles {
            }
            installations {
            }
            tests {
            }
        }

        // find android tool
        def antTool = GradleSpacelift.tools("android")
        assertThat antTool, is(notNullValue())
    }

    @Test
    public void binaryAsMapOfArrays() {
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'spacelift'

        project.ext.set("androidHome", "foobar")

        project.spacelift {
            tools {
                android {
                    command ([
                        linux: [
                            new File(project.androidHome, "tools/android.bat").getAbsolutePath()
                        ],
                        windows: [
                            "cmd.exe",
                            "/C",
                            new File(project.androidHome, "tools/android.bat").getAbsolutePath()
                        ]
                    ])
                }
            }
            profiles {
            }
            installations {
            }
            tests {
            }
        }

        // find android tool
        def antTool = GradleSpacelift.tools("android")
        assertThat antTool, is(notNullValue())
    }
}
