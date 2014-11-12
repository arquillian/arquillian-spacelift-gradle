package org.arquillian.spacelift.gradle

import groovy.lang.Closure
import groovy.lang.Mixin

import org.gradle.api.Project

// this class represents a profile enumerating installations to be installed
@Mixin(ValueExtractor)
class Profile {

    // this is required in order to use project container abstraction
    final String name

    // list of enabled installations
    Closure enabledInstallations = { []}

    // list of tests to execute
    Closure tests = { []}

    Project project

    Profile(String profileName, Project project) {
        this.name = profileName
        this.project = project
    }

    def enabledInstallations(Object... args) {
        this.enabledInstallations = extractValueAsLazyClosure(args).dehydrate()
        this.enabledInstallations.resolveStrategy = Closure.DELEGATE_FIRST
    }

    def getEnabledInstallations() {
        def enabledInstallationsList = enabledInstallations.rehydrate(new GradleSpaceliftDelegate(), this, this).call()
        if(enabledInstallationsList==null) {
            return []
        }
        return enabledInstallationsList
    }

    def tests(Object... args) {
        this.tests = extractValueAsLazyClosure(arg).dehydrate()
        this.tests.resolveStrategy = Closure.DELEGATE_FIRST
    }

    def getTests() {
        def enabledTestList = tests.rehydrate(new GradleSpaceliftDelegate(), this, this).call()
        if(enabledTestList==null) {
            return []
        }
        return enabledTestList
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

        return sb.toString()
    }
}
