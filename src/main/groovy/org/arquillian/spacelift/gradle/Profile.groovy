package org.arquillian.spacelift.gradle

import org.gradle.api.Project

// this class represents a profile enumerating installations to be installed

class Profile extends BaseContainerizableObject<Profile> implements ContainerizableObject<Profile> {

    // list of enabled installations
    Closure enabledInstallations = { []}

    // list of tests to execute
    Closure tests = { []}

    // list of tests to exclude
    Closure excludedTests = { [] }

    Profile(String profileName, Project project) {
        super(profileName, project)
    }

    /**
     * Cloning constructor. Preserves lazy nature of closures to be evaluated later on.
     * @param other Profile to be cloned
     */
    Profile(String profileName, Profile other) {
        super(profileName, other)

        // use direct access to skip call of getter
        this.enabledInstallations = other.@enabledInstallations.clone()
        this.tests = other.@tests.clone()
        this.excludedTests = other.@excludedTests.clone()
    }

    @Override
    public Profile clone(String name) {
        return new Profile(name, this)
    }

    def getEnabledInstallations() {
        def enabledInstallationsList = enabledInstallations.rehydrate(new GradleSpaceliftDelegate(), this, this).call()
        return asFlatList(enabledInstallationsList)
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
