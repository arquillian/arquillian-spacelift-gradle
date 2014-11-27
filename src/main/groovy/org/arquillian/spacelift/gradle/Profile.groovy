package org.arquillian.spacelift.gradle

import org.gradle.api.Project

// this class represents a profile enumerating installations to be installed
class Profile implements ValueExtractor{

    // this is required in order to use project container abstraction
    final String name

    // list of enabled installations
    Closure enabledInstallations = { []}

    // list of tests to execute
    Closure tests = { []}

    // list of tests to exclude
    Closure excludedTests = { []}
    
    Project project

    Profile(String profileName, Project project) {
        this.name = profileName
        this.project = project
    }

    def enabledInstallations(Object... args) {
        this.enabledInstallations = extractValuesAsLazyClosure(args).dehydrate()
        this.enabledInstallations.resolveStrategy = Closure.DELEGATE_FIRST
    }

    def getEnabledInstallations() {
        def enabledInstallationsList = enabledInstallations.rehydrate(new GradleSpaceliftDelegate(), this, this).call()
        if(enabledInstallationsList==null) {
            return []
        }
        return enabledInstallationsList.flatten()
    }

    def tests(Object... args) {
        this.tests = extractValuesAsLazyClosure(args).dehydrate()
        this.tests.resolveStrategy = Closure.DELEGATE_FIRST
    }

    def excludedTests(Object... args) {
        this.excludedTests = extractValuesAsLazyClosure(args).dehydrate()
        this.tests.resolveStrategy = Closure.DELEGATE_FIRST
    }
    
    def getTests() {
        def enabledTestList = tests.rehydrate(new GradleSpaceliftDelegate(), this, this).call()
        if(enabledTestList==null) {
            return []
        }
        return enabledTestList.flatten()
    }

    def getExcludedTests() {
        def excludedTestList = excludedTests.rehydrate(new GradleSpaceliftDelegate(), this, this).call()
        if (excludedTestList == null) {
            return []
        }
        return excludedTestList.flatten()
    }
    
    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder()
        sb.append("Profile: ").append(name).append("\n")

        // installations
        sb.append("\tInstallations: ")
        getEnabledInstallations().each {
            sb.append(it).append(" ")
        }
        sb.append("\n")

        sb.append("\tTests: ")
        getTests().each {
            sb.append(it).append(" ")
        }
        
        sb.append("\tExcluded tests: ")
        getExcludedTests().each {
            sb.append(it).append(" ")
        }

        return sb.toString()
    }
}
