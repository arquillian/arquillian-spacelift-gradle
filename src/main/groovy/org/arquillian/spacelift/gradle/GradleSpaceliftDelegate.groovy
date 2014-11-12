package org.arquillian.spacelift.gradle

import org.arquillian.spacelift.execution.Tasks
import org.arquillian.spacelift.execution.impl.DefaultExecutionServiceFactory
import org.arquillian.spacelift.tool.ToolRegistry
import org.arquillian.spacelift.tool.impl.ToolRegistryImpl
import org.gradle.api.Project

class GradleSpaceliftDelegate {

    private static final class ProjectHolder {
        private static Project project;
        private static ToolRegistry tools;
    }

    public static void init() {
        ProjectHolder.project = GradleSpacelift.currentProject()
        ProjectHolder.tools = GradleSpacelift.toolRegistry()
    }
    
    /*
    def project() {
        if(ProjectHolder.tools==null) {
            throw new IllegalStateException("Current project was not set via plugin.")
        }
        return ProjectHolder.project
    }   
    */ 

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
}
