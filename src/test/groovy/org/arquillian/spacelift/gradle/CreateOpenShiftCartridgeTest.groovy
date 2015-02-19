package org.arquillian.spacelift.gradle

import org.arquillian.spacelift.Spacelift
import org.arquillian.spacelift.process.ProcessResult
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.arquillian.spacelift.gradle.openshift.CreateOpenShiftCartridge;
import org.arquillian.spacelift.gradle.utils.KillJavas
import org.junit.Ignore;
import org.junit.Test

import static org.hamcrest.CoreMatchers.*
import static org.junit.Assert.assertThat

/**
 * Creates OpenShift cartridge. Requires to have authorization token for rhc tools
 * @author <a href="mailto:kpiwko@redhat.com">Karel Piwko</a>
 *
 */
@Ignore
public class CreateOpenShiftCartridgeTest {

    @Test
    public void createCart() {
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'spacelift'

        project.spacelift {
            tools { rhc { command "rhc" } }
            profiles {
            }
            installations {
            }
            tests {
            }
        }

        // kill servers
        Spacelift.task(CreateOpenShiftCartridge)
                .named('foobar')
                .at('mobileqa')
                .sized("small")
                .cartridges("https://cartreflect-claytondev.rhcloud.com/reflect?github=matzew/openshift-origin-cartridge-aerogear-push", "mysql-5.5")
                .force()
                .execute().await()
    }
}
