package org.arquillian.spacelift.gradle

import org.arquillian.spacelift.gradle.GradleSpacelift.ProjectHolder;
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException

import static org.hamcrest.CoreMatchers.is
import static org.hamcrest.CoreMatchers.notNullValue
import static org.junit.Assert.assertThat

/**
 * Asserts that installations can be specified without home
 * @author <a href="mailto:kpiwko@redhat.com">Karel Piwko</a>
 *
 */
public class ProfileParsingTest {

    @Test
    void "single profile with single installation and no tests"() {
        Project project = initWithProfile { enabledInstallations "eap" }

        assertThat project.selectedInstallations.size(), is(1)
    }

    @Test
    void "single profile with single installation as array and no tests"() {
        Project project = initWithProfile { enabledInstallations(["eap"]) }

        assertThat project.selectedInstallations.size(), is(1)
    }

    @Test
    void "single profile with multiple installations as args and no tests"() {
        Project project = initWithProfile { enabledInstallations 'eap', 'ews' }

        assertThat project.selectedInstallations.size(), is(2)
    }

    @Test
    void "single profile with multiple installations as args and single test"() {
        Project project = initWithProfile {
            enabledInstallations 'eap', 'ews'
            tests 'fooTest'
        }

        assertThat project.selectedInstallations.size(), is(2)
        assertThat project.selectedTests.size(), is(1)
    }

    @Test
    void "single profile with multiple installations and tests unquoted"() {
        Project project = initWithProfile {
            enabledInstallations eap, ews
            tests fooTest
        }

        assertThat project.selectedInstallations.size(), is(2)
        assertThat project.selectedTests.size(), is(1)
    }

    @Test
    void "single profile with multiple installations as array and multiple tests as array"() {
        def project = initWithProfile {
            enabledInstallations(['eap', 'ews'])
            tests(['fooTest', 'barTest'])
        }

        assertThat project.selectedInstallations.size(), is(2)
        assertThat project.selectedTests.size(), is(2)
    }

    @Test
    void "single profile with single installation and single test and single excluded test as closure"() {
        Project project = initWithProfile {
            enabledInstallations { 'eap' }
            tests { 'fooTest' }
            excludedTests { 'barTest' }
        }

        assertThat project.selectedInstallations.size(), is(1)
        assertThat project.selectedTests.size(), is(1)
    }

    @Test
    void "single profile with single installation and single test and single excluded test as array in closure"() {
        initWithProfile {
            enabledInstallations { ['eap']}
            tests { ['fooTest']}
            excludedTests { ['barTest']}
        }
    }

    @Test
    void "single profile with single installation and single test and single excluded test as Java array in closure"() {
        initWithProfile {
            enabledInstallations {
                String[] args = new String[1];
                args[0] = 'eap'
                return args
            }
            tests {
                String[] args = new String[1];
                args[0] = 'fooTest'
                return args
            }
            excludedTests {
                String[] args = new String[1];
                args[0] = 'barTest'
                return args
            }
        }
    }

    @Test
    void "single profile with single installation and single test and single excluded test as ArrayList in closure"() {
        initWithProfile {
            enabledInstallations {
                ArrayList<String> args = new ArrayList<>();
                args.add('eap')
                return args
            }
            tests {
                ArrayList<String> args = new ArrayList<>();
                args.add('fooTest')
                return args
            }
            excludedTests {
                ArrayList<String> args = new ArrayList<>();
                args.add('barTest')
                return args
            }
        }
    }

    @Test
    void "single profile with single installation and single test and single excluded test as List in closure"() {
        initWithProfile {
            enabledInstallations { Arrays.asList('eap') }
            tests { Arrays.asList('fooTest') }
            excludedTests { Arrays.asList('barTest') }
        }
    }

    @Test
    void "single profile with multiple installations as array in closure and no tests"() {
        initWithProfile {
            enabledInstallations { ['eap', 'ews']}
        }
    }

    @Test
    void "single profile with single installation as array in closure and single test as array in closure"() {
        initWithProfile {
            enabledInstallations { ['eap']}
            tests { ['fooTest']}
        }
    }

    Project initWithProfile(Closure profileDefinition) {
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'spacelift'

        // enable foobar profile
        project.ext.set("foobar", "true")

        project.spacelift {
            tools { rhc { command "rhc" } }
            profiles { foobar profileDefinition }
            installations {
                eap {}
                ews {}
            }
            tests {
                fooTest {}
                barTest {}
            }
        }

        project.tasks['init'].execute()

        project.spacelift.installations.each { installation ->
            assertThat installation.home, is(notNullValue())
            assertThat installation.home.exists(), is(true)
        }

        assertThat project.selectedProfile, is(notNullValue())
        assertThat project.selectedProfile.name, is('foobar')

        return project
    }
}
