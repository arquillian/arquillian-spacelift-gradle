package org.arquillian.spacelift.gradle;

import static org.junit.Assert.*

import org.arquillian.spacelift.Spacelift
import org.arquillian.spacelift.gradle.utils.RHELVersion
import org.junit.Test

class RHELVersionTest {

    @Test
    public void testRHELVersion() {
        def rhelVersionFile = new File("/etc/redhat-release")

        def version = Spacelift.task(RHELVersion).execute().await()

        if (version.equals("no RHEL")) {
            assertTrue (!rhelVersionFile.exists() || !contains("Red Hat", rhelVersionFile))
        } else {
            assertTrue contains(version, rhelVersionFile)
        }
    }

    def boolean contains(release, file) {

        def found = false

        file.eachLine {
            if (it.contains(release)) {
                found = true
            }
        }

        found
    }
}
