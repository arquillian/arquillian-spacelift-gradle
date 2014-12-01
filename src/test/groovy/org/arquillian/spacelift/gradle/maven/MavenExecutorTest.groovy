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
    void "propagate env property by key and value"() {

        Assume.assumeThat EnvironmentUtils.runsOnLinux(), is(true)
        def maven = instantiateSimpleProjectWithMavenExecutor()


        ProcessResult result = maven.env("HEY_SPACELIFT", "'This is plugin!'").goal("help:system").execute().await()
        assertThat result.output().find { line -> line.contains("HEY_SPACELIFT")}, is(notNullValue())
    }
    
    @Test
    void "propagate env property by pair"() {

        Assume.assumeThat EnvironmentUtils.runsOnLinux(), is(true)
        def maven = instantiateSimpleProjectWithMavenExecutor()


        ProcessResult result = maven.env([HEY_SPACELIFT: "'This is plugin!'"]).goal("help:system").execute().await()
        assertThat result.output().find { line -> line.contains("HEY_SPACELIFT")}, is(notNullValue())
    }
    
    @Test
    void "propagete env property with GString value"() {
        Assume.assumeThat EnvironmentUtils.runsOnLinux(), is(true)
        def maven = instantiateSimpleProjectWithMavenExecutor()

        def hello = "HelloWorld!"

        ProcessResult result = maven.env([HEY_SPACELIFT: "${hello}"]).goal("help:system").execute().await()
        assertThat result.output().find { line -> line.contains("HEY_SPACELIFT")}, is(notNullValue())
    }
    
    @Test
    void "propagete env property with GString key and value"() {
        Assume.assumeThat EnvironmentUtils.runsOnLinux(), is(true)
        def maven = instantiateSimpleProjectWithMavenExecutor()

        def hellokey= "HEY_SPACELIFT"
        def hello = "HelloWorld!"

        ProcessResult result = maven.env(["${hellokey}": "${hello}"]).goal("help:system").execute().await()
        assertThat result.output().find { line -> line.contains("HEY_SPACELIFT")}, is(notNullValue())
    }
    

    private MavenExecutor instantiateSimpleProjectWithMavenExecutor() {
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
        
        maven
    }
}