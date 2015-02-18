package org.arquillian.spacelift.gradle

import groovy.transform.CompileStatic

import org.gradle.api.Project

// this class represents a profile enumerating installations to be installed

@CompileStatic
class Profile extends BaseContainerizableObject<Profile> implements ContainerizableObject<Profile> {

    // list of enabled installations
    DeferredValue<List> enabledInstallations = DeferredValue.of(List.class).from([])

    // list of tests to execute
    DeferredValue<List> tests = DeferredValue.of(List.class).from([])

    // list of tests to exclude
    DeferredValue<List> excludedTests = DeferredValue.of(List.class).from([])

    Profile(String profileName, Object parent) {
        super(profileName, parent)
    }

    /**
     * Cloning constructor. Preserves lazy nature of closures to be evaluated later on.
     * @param other Profile to be cloned
     */
    Profile(String profileName, Profile other) {
        super(profileName, other)

        // use direct access to skip call of getter
        this.enabledInstallations = other.@enabledInstallations.copy()
        this.tests = other.@tests.copy()
        this.excludedTests = other.@excludedTests.copy()
    }

    @Override
    public Profile clone(String name) {
        return new Profile(name, this)
    }

    List<String> getEnabledInstallations() {
        return enabledInstallations.resolve()
    }

    List<String> getTests() {
        return tests.resolve()
    }

    List<String> getExcludedTests() {
        return excludedTests.resolve()
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
