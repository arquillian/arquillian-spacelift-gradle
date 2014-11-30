package org.arquillian.spacelift.gradle

import org.gradle.api.Project

// this class represents a profile enumerating installations to be installed

class Profile implements ValueExtractor, Cloneable {

    // this is required in order to use project container abstraction
    String name

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

    /**
     * Cloning constructor. Preserves lazy nature of closures to be evaluated later on.
     * @param other Profile to be cloned
     */
    Profile(Profile other) {

        // use direct access to skip call of getter
        this.enabledInstallations = other.@enabledInstallations.clone()
        this.tests = other.@tests.clone()
        this.excludedTests = other.@excludedTests.clone()

        // shallow copy of project
        this.project = other.project
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        new Profile(this)
    }

    def enabledInstallations(Object... args) {
        this.enabledInstallations = extractValuesAsLazyClosure(args).dehydrate()
    }

    def getEnabledInstallations() {
        def enabledInstallationsList = enabledInstallations.rehydrate(new GradleSpaceliftDelegate(), this, this).call()
        return asFlatList(enabledInstallationsList)
    }

    def tests(Object... args) {
        this.tests = extractValuesAsLazyClosure(args).dehydrate()
    }

    def excludedTests(Object... args) {
        this.excludedTests = extractValuesAsLazyClosure(args).dehydrate()
    }

    def getTests() {
        def enabledTestList = tests.rehydrate(new GradleSpaceliftDelegate(), this, this).call()
        return asFlatList(enabledTestList)
    }

    def getExcludedTests() {
        def excludedTestList = excludedTests.rehydrate(new GradleSpaceliftDelegate(), this, this).call()
        return asFlatList(excludedTestList)
    }


    static asFlatList(charSequenceOrCollection) {
        if(charSequenceOrCollection == null) {
            return []
        } else if(charSequenceOrCollection instanceof CharSequence) {
            return [charSequenceOrCollection]
        } else {
            return charSequenceOrCollection.flatten()
        }
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
