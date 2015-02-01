package org.arquillian.spacelift.gradle

import static org.hamcrest.CoreMatchers.*
import static org.junit.Assert.assertThat

import org.arquillian.spacelift.Spacelift
import org.arquillian.spacelift.process.CommandBuilder
import org.arquillian.spacelift.process.ProcessResult
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Assume
import org.junit.Test


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
        def antTool = Spacelift.task("ant")
        assertThat antTool, is(notNullValue())

        // call ant help
        Spacelift.task("ant").parameters("-help").interaction(GradleSpaceliftDelegate.ECHO_OUTPUT).execute().await()
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
        def antTool = Spacelift.task("ant")
        assertThat antTool, is(notNullValue())

        // call ant help
        Spacelift.task("ant").parameters("-help").execute().await()
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
        def antTool = Spacelift.task("ant")
        assertThat antTool, is(notNullValue())

        // call ant help
        Spacelift.task("ant").parameters("-help").execute().await()

        // find mvn tool
        def mvnTool = Spacelift.task("mvn")
        assertThat mvnTool, is(notNullValue())

        // call mvn help
        Spacelift.task("mvn").parameters("-help").execute().await()

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
        def antTool = Spacelift.task("ant")
        assertThat antTool, is(notNullValue())

        // call ant help
        Spacelift.task("ant").parameters("-help").execute().await()
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
        def antTool = Spacelift.task("ant")
        assertThat antTool, is(notNullValue())

        // call ant help
        Spacelift.task("ant").parameters("-help").execute().await()
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
        def antTool = Spacelift.task("ant")
        assertThat antTool, is(notNullValue())

        // call ant help
        ProcessResult result = Spacelift.task("ant").parameters("-help").execute().await()
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
        def antTool = Spacelift.task("android")
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
        def antTool = Spacelift.task("android")
        assertThat antTool, is(notNullValue())
    }
}
