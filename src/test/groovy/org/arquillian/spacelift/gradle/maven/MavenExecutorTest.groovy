package org.arquillian.spacelift.gradle.maven

import static org.hamcrest.CoreMatchers.*
import static org.junit.Assert.assertThat

import org.arquillian.spacelift.execution.Tasks
import org.arquillian.spacelift.gradle.GradleSpacelift;
import org.arquillian.spacelift.gradle.maven.MavenExecutor
import org.arquillian.spacelift.gradle.utils.EnvironmentUtils
import org.arquillian.spacelift.process.CommandBuilder
import org.arquillian.spacelift.process.ProcessResult;
import org.arquillian.spacelift.process.impl.CommandTool
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Assume
import org.junit.Test

class MavenExecutorTest {

    @Test
    void "propagate env property"() {

        Assume.assumeThat EnvironmentUtils.runsOnLinux(), is(true)

        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'spacelift'

        project.spacelift {
            tools {
                mvn {
                    command {
                        Tasks.prepare(CommandTool).command(new CommandBuilder("mvn"))
                    }
                }
            }
        }

        // call mvn help
        GradleSpacelift.tools("mvn").parameters("-help").interaction(GradleSpacelift.ECHO_OUTPUT).execute().await()

        // find and execute maven executor
        def maven = Tasks.prepare(MavenExecutor)
        assertThat maven, is(notNullValue())

        ProcessResult result = maven.env("HEY_SPACELIFT", "'This is plugin!'").goal("help:system").execute().await()
        assertThat result.output().find { line -> line.contains("HEY_SPACELIFT")}, is(notNullValue())
    }
}
