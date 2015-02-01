package org.arquillian.spacelift.gradle.git

import org.arquillian.spacelift.task.Task


/**
 * Default GIT_SSH file does not check host key of a remote host so when used with git tools,
 * these tools are not interactive hence not blocking.
 *
 * @author <a href="mailto:smikloso@redhat.com">Stefan Miklosovic</a>
 *
 */
class GitSshFileTask extends Task<Object, File> {

    private File toFile

    def toFile(File toFile) {
        this.toFile = toFile
        this
    }

    @Override
    protected File process(Object input) throws Exception {

        def sshFile = toFile

        if (!sshFile) {
            sshFile = new File(System.getProperty("java.io.tmpdir"), "gitssh.sh")
        }

        if (!sshFile.exists()) {
            this.getClass().getResource("/scripts/gitssh.sh").withInputStream { ris ->
                sshFile.withOutputStream { fos -> fos << ris }
            }
        }

        sshFile.setExecutable(true)

        sshFile
    }
}
