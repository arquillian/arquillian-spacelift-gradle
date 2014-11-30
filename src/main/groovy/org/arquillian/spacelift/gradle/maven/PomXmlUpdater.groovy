package org.arquillian.spacelift.gradle.maven

import org.arquillian.spacelift.execution.Task
import org.arquillian.spacelift.execution.Tasks
import org.arquillian.spacelift.gradle.GradleSpacelift
import org.arquillian.spacelift.gradle.xml.XmlFileLoader
import org.arquillian.spacelift.gradle.xml.XmlUpdater
import org.gradle.api.Project
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PomXmlUpdater extends Task<Object, Void> {
    private static final Logger log = LoggerFactory.getLogger(PomXmlUpdater)

    Set<File> xmlFiles = new LinkedHashSet<File>()

    private Project project

    PomXmlUpdater() {
        this.project = GradleSpacelift.currentProject()
    }

    /**
     * Calls dir() with default includes and excludes patterns
     */
    PomXmlUpdater dir(Object directory) {
        dir(dir:directory)
    }

    /**
     * Allows to further define includes and excludes pattern for the directory.
     *
     * Map accepts following arguments:
     * dir - directory to be scanned, can be File or String
     * includes - array of includes in Ant format
     * excludes - array of excludes in Ant format
     */
    PomXmlUpdater dir(Map args) {
        def dir = args.get('dir')
        def includes = args.get('includes', [
            "**/pom.xml"
        ])
        def excludes = args.get('excludes', [
            "${project.spacelift.localRepository}/**",
            "**/target/**"
        ])

        // skip if directory is invalid or non existing
        if(dir==null) {
            return this
        }

        // get all arquillian.xml files in directory
        project.fileTree("${dir}") {
            include includes
            exclude excludes
        }.each { file ->
            xmlFiles << file
            log.debug("Adding ${file} to update batch, total ${xmlFiles.size()}")
        }

        this
    }

    @Override
    protected Void process(Object properties) throws Exception {

        xmlFiles.each { file ->
            def pom = Tasks.chain(file, XmlFileLoader).execute().await()

            properties.each { propertyKey, value ->
                pom.properties.each { p ->
                    p.value().each { v ->
                        if (v.name() == propertyKey) {
                            v.value = value
                        }
                    }
                }
            }

            Tasks.chain(pom, XmlUpdater).file(file).execute().await()
        }

        return null
    }
}
