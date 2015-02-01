package org.arquillian.spacelift.gradle.arquilian

import org.arquillian.spacelift.Spacelift
import org.arquillian.spacelift.gradle.arquillian.ArquillianXmlUpdater
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.BeforeClass
import org.junit.Test

class ArquillianXmlUpdaterTest {

    @BeforeClass
    public static void initializeSpacelift() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'spacelift'
    }

    @Test
    void "modify arquillian.xml files"() {
        Spacelift.task(
                [foo:'bar'], ArquillianXmlUpdater)
                .dir(new File('.'))
                .execute().await()
    }

    @Test
    void "modify arquillian.xml files with includes, excludes"() {
        Spacelift.task(
                [foo:'bar'], ArquillianXmlUpdater)
                .dir(dir:new File('.'), includes:["foo/arquillian.xml"], excludes:["**/target/**"])
                .execute().await()
    }
}
