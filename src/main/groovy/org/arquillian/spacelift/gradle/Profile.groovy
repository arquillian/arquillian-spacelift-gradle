package org.arquillian.spacelift.gradle

import org.gradle.api.Project

// this class represents a profile enumerating installations to be installed

class Profile extends BaseContainerizableObject<Profile> implements ContainerizableObject<Profile> {

    // list of enabled installations
    Closure enabledInstallations = { [] }

    // list of tests to execute
    Closure tests = { [] }

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

    List<String> getEnabledInstallations() {
        return DSLUtil.resolve(List.class, enabledInstallations, this)
    }

    List<String> getTests() {
        return DSLUtil.resolve(List.class, tests, this)
    }

    List<String> getExcludedTests() {
        return DSLUtil.resolve(List.class, excludedTests, this)
    }

    /**
     * Allows us to reference profiles and test without quoting
     * @param name installation or test name
     * @return string value of the property
     */
    // FIXME this is causing issues with DSL, likely it should rather be defined in Profiles container
    //def propertyMissing(String name) {
    //    return name
    //}

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
