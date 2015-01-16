package org.arquillian.spacelift.gradle

import org.arquillian.spacelift.execution.Tasks
import org.arquillian.spacelift.execution.impl.DefaultExecutionServiceFactory
import org.arquillian.spacelift.tool.ToolRegistry
import org.arquillian.spacelift.tool.impl.ToolRegistryImpl
import org.gradle.api.Project
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class GradleSpaceliftDelegate {
    private static final Logger log = LoggerFactory.getLogger(GradleSpaceliftDelegate)

    private static final class ProjectHolder {
        private static Project project;
        private static ToolRegistry tools;
    }

    public static void init() {
        ProjectHolder.project = GradleSpacelift.currentProject()
        ProjectHolder.tools = GradleSpacelift.toolRegistry()
    }

    def project() {
        if(ProjectHolder.project==null) {
            throw new IllegalStateException("Current project was not set via plugin.")
        }
        return ProjectHolder.project
    }

    def tool(String toolName) {
        if(ProjectHolder.tools==null) {
            throw new IllegalStateException("Current project was not set via plugin.")
        }
        return ProjectHolder.tools.find(toolName);
    }

    def tool(Class classType) {
        return Tasks.prepare(classType)
    }

    def tool(Class classType, Closure inputData) {
        return Tasks.chain(inputData.call(), classType)
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
