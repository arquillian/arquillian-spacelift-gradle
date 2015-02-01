package org.arquillian.spacelift.gradle

import org.arquillian.spacelift.Spacelift
import org.arquillian.spacelift.process.ProcessInteraction
import org.arquillian.spacelift.process.ProcessInteractionBuilder
import org.gradle.api.Project
import org.slf4j.Logger
import org.slf4j.LoggerFactory

// FIXME this is temporary delegate object that should be removed
class GradleSpaceliftDelegate {

    private static final Logger log = LoggerFactory.getLogger(GradleSpaceliftDelegate)

    public static final ProcessInteraction ECHO_OUTPUT = new ProcessInteractionBuilder().outputPrefix("").when("(?s).*").printToOut().build()

    private static final class ProjectHolder {
        private static Project project;
    }

    public static void currentProject(Project project) {
        ProjectHolder.project = project;
    }

    Project project() {
        if(ProjectHolder.project==null) {
            throw new IllegalStateException("Current project was not set via plugin.")
        }
        return ProjectHolder.project
    }

    def tool(String toolName) {
        return Spacelift.task(toolName)
    }

    def tool(Class classType) {
        return Spacelift.task(classType)
    }

    def tool(Class classType, Closure inputData) {
        return Spacelift.task(inputData.call(), classType)
    }

    def installation(String name) {
        if(ProjectHolder.project==null) {
            throw new IllegalStateException("Current project was not set via plugin.")
        }
        return ProjectHolder.project.spacelift.installations[name];
    }

    def test(String name) {
        if(ProjectHolder.project==null) {
            throw new IllegalStateException("Current project was not set via plugin.")
        }
        return ProjectHolder.project.spacelift.tests[name];
    }

    def propertyMissing(String name) {
        if(ProjectHolder.project==null) {
            throw new IllegalStateException("Current project was not set via plugin.")
        }

        // iterate over Spacelift structures
        def spacelift = ProjectHolder.project.spacelift
        if(!spacelift) {
            throw new IllegalStateException("Current project is not a Spacelift Project")
        }

        def object, objectType
        for(def container : ([
            // order here defines order of reference in case reference to the same object is found, laters are ignored
            spacelift.installations,
            spacelift.tests,
            spacelift.tools,
            spacelift.profiles,
        ]
        )) {
            def resolved = resolve(container, name)
            if(object==null && resolved != null) {
                log.debug("Resolved ${container.type.getSimpleName()} named ${name}")
                object = resolved
                objectType = container.type
            }
            else if(object!=null && resolved != null) {
                log.warn("Detected ambiguous reference ${name}, using ${objectType.getSimpleName()}, ignoring ${container.type.getSimpleName()}")
            }
        }

        if(object!=null) {
            return object
        }

        // pass resolution to parent
        throw new MissingPropertyException("Unable to resolve property named ${name} in Spacelift DSL")
    }

    private <TYPE extends ContainerizableObject<TYPE>, DEFAULT_TYPE extends TYPE> TYPE resolve(InheritanceAwareContainer<TYPE, DEFAULT_TYPE> container, String name) {
        try {
            return container.getAt(name)
        }
        catch(MissingPropertyException e) {
            log.debug("Unable to resolve ${container.type.getSimpleName()} named ${name}")
            return null
        }
    }

}
