package org.arquillian.spacelift.gradle.maven

import org.arquillian.spacelift.Spacelift
import org.arquillian.spacelift.gradle.GradleSpaceliftDelegate
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.BeforeClass
import org.junit.Test

import org.arquillian.spacelift.gradle.SpaceliftPlugin

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
            SpaceliftPlugin.installInstallation(installation, project.logger)
        }
    }

    @Test
    void "modify pom.xml files"() {
        Spacelift.task(
                [foo:'bar'], PomXmlUpdater)
                .dir(new GradleSpaceliftDelegate().project().spacelift.workspace)
                .execute().await()
    }

    @Test
    void "modify pom.xml files dir specified by path"() {
        Spacelift.task(
                [foo:'bar'], PomXmlUpdater)
                .dir("${new GradleSpaceliftDelegate().project()}.spacelift.workspace}")
                .execute().await()
    }

    @Test
    void "modify pom.xml files with includes, excludes"() {
        Spacelift.task(
                [foo:'bar'], PomXmlUpdater)
                .dir(dir:new File('.'), includes:["**/pomx.xml"], excludes:["**/target/**"])
                .execute().await()
    }
}
