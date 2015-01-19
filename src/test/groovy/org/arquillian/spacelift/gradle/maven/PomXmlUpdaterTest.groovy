package org.arquillian.spacelift.gradle.maven

import org.arquillian.spacelift.execution.Tasks
import org.arquillian.spacelift.execution.impl.DefaultExecutionServiceFactory
import org.arquillian.spacelift.gradle.GradleSpacelift;
import org.arquillian.spacelift.gradle.arquillian.ArquillianXmlUpdater
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.BeforeClass
import org.junit.Test

class PomXmlUpdaterTest {

    @BeforeClass
    public static void initializeSpacelift() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'spacelift'

        project.spacelift {
            installations {
                foo {
                    product "test"
                    version "1"
                    remoteUrl "https://github.com/arquillian/arquillian-selenium-bom/archive/master.zip"
                    home "mydirectory"
                    extractMapper {
                        toDir("${home}")
                        cutdirs()
                    }
                }
            }
        }

        project.spacelift.installations.each { installation ->
            installation.install(project.logger)
        }
    }

    @Test
    void "modify pom.xml files"() {
        Tasks.chain(
                [foo:'bar'], PomXmlUpdater)
                .dir(GradleSpacelift.currentProject().spacelift.workspace)
                .execute().await()
    }

    @Test
    void "modify pom.xml files dir specified by path"() {
        Tasks.chain(
                [foo:'bar'], PomXmlUpdater)
                .dir("${GradleSpacelift.currentProject().spacelift.workspace}")
                .execute().await()
    }

    @Test
    void "modify pom.xml files with includes, excludes"() {
        Tasks.chain(
                [foo:'bar'], PomXmlUpdater)
                .dir(dir:new File('.'), includes:["**/pomx.xml"], excludes:["**/target/**"])
                .execute().await()
    }
}
