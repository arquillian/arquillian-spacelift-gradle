package org.arquillian.spacelift.gradle.arquillian

import org.arquillian.spacelift.execution.Task
import org.arquillian.spacelift.execution.Tasks
import org.arquillian.spacelift.gradle.GradleSpacelift
import org.arquillian.spacelift.gradle.xml.XmlFileLoader
import org.arquillian.spacelift.gradle.xml.XmlUpdater
import org.gradle.api.Project
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ArquillianXmlUpdater extends Task<Object, Void>{
    private static final Logger log = LoggerFactory.getLogger(ArquillianXmlUpdater)

    Set<File> arquillianXmlFiles = new LinkedHashSet<File>()

    def containers = []

    def extensions = []

    private Project project

    ArquillianXmlUpdater() {
        this.project = GradleSpacelift.currentProject()
    }

    /**
     * Calls dir(Map) with default includes and excludes patterns
     */
    ArquillianXmlUpdater dir(Object directory) {
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
    ArquillianXmlUpdater dir(Map args) {

        def dir = args.get('dir')
        def includes = args.get('includes', [
            "**/arquillian.xml",
            "**/arquillian-domain.xml"
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
            arquillianXmlFiles << file
            log.debug("Adding ${file} to update batch, total ${arquillianXmlFiles.size()}")
        }

        this
    }

    ArquillianXmlUpdater containers(CharSequence...containers) {
        this.containers.addAll(containers)
        this
    }

    ArquillianXmlUpdater container(String container) {
        this.containers << container
        this
    }

    ArquillianXmlUpdater extensions(CharSequence...extensions) {
        this.extensions.addAll(extensions)
        this
    }

    ArquillianXmlUpdater extension(String extension) {
        this.extensions << extension
        this
    }


    @Override
    protected Void process(Object properties) throws Exception {

        if(!containers.isEmpty()) {
            configureContainer(containers, properties);
        }

        if(!extensions.isEmpty()) {
            configureExtension(extensions, properties);
        }

        return null;
    }

    def configureContainer(def containers, def properties) {
        arquillianXmlFiles.each { arquillianXml ->
            configureContainer(containers, properties, arquillianXml)
        }
    }

    def configureContainer(def containers, def properties, File arquillianXml) {

        def arquillian = Tasks.chain(arquillianXml, XmlFileLoader).execute().await()

        containers.each { container ->
            log.debug("Modifying container \"*${container}*\" configuration(s) in ${arquillianXml}")

            // replace standalone <container>s
            arquillian.container.findAll {c -> c.@qualifier.contains(container)}.configuration.each { configuration ->
                properties.each { key, value ->
                    // remove existing properties
                    configuration.property.findAll { p -> p.@name == "${key}"}.each { it.replaceNode {}}
                    // put new configuration properties
                    configuration*.append(new Node(null, 'property', [name: "${key}"], "${value}"))
                }
            }
            // replace containers in <group>
            arquillian.group.container.findAll {c -> c.@qualifier.contains(container)}.configuration.each { configuration ->
                properties.each { key, value ->
                    // remove existing properties
                    configuration.property.findAll { p -> p.@name == "${key}"}.each { it.replaceNode {} }
                    // put new configuration properties
                    configuration*.append(new Node(null, 'property', [name: "${key}"], "${value}"))
                }
            }
        }
        Tasks.chain(arquillian, XmlUpdater).file(arquillianXml).execute().await()
    }

    def configureExtension(def extensions, def properties) {
        arquillianXmlFiles.each { arquillianXml ->
            configureExtension(extensions, properties, arquillianXml)
        }
    }

    def configureExtension(def extensions, def properties, File arquillianXml) {

        def arquillian = Tasks.chain(arquillianXml, XmlFileLoader).execute().await()
        extensions.each { extensionQualifier ->
            log.debug("Modifying Arquillian extension \"${extensionQualifier}\" configuration(s) at ${arquillianXml}")

            arquillian.extension.findAll {e -> e.@qualifier == "${extensionQualifier}"}.each { extension ->
                properties.each { key, value ->
                    // remove existing property
                    extension.property.findAll { p -> p.@name == "${key}"}.each { it.replaceNode {} }
                    // put new property
                    extension.append(new Node(null, 'property', [name: "${key}"], "${value}"))
                }
            }
        }
        Tasks.chain(arquillian, XmlUpdater).file(arquillianXml).execute().await()
    }
}
