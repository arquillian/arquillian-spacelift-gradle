package org.arquillian.spacelift.gradle.git

import java.io.File

import org.arquillian.spacelift.execution.Task
import org.arquillian.spacelift.execution.Tasks

/**
 * Default GIT_SSH file does not check host key of a remote host so when used with git tools, 
 * these tools are not interactive hence not blocking.
 * 
 * @author <a href="mailto:smikloso@redhat.com">Stefan Miklosovic</a>
 *
 */
class GitSshFileTask extends Task<Object, File> {

    private File file

    private File sshFile

    /**
     * 
     * @param file file where GIT_SSH script will be written
     * @return
     */
    GitSshFileTask to(File file) {
        if (file) {
            this.file = file
        }
        this
    }

    /**
     * 
     * @param file file with GIT_SSH script to write to {@link #to(File)}.
     * @return
     */
    GitSshFileTask from(File file) {
        if (file && file.exists() && file.isFile()) {
            this.sshFile = sshFile
        }
        this
    }

    @Override
    protected File process(Object input) throws Exception {

        File writeFile

        if (!file) {
            writeFile = new File(System.getProperty("java.io.tmpdir"), "gitssh.sh")
        } else {
            writeFile = file
        }

        InputStream sshFile

        if (!this.sshFile) {
            sshFile = getStream()
        } else {
            if (!this.sshFile.exists()) {
                sshFile = getStream()
            } else {
                sshFile = new FileInputStream(this.sshFile)
            }
        }

        if (!writeFile.exists()) {
            getFile(sshFile, writeFile)
        }

        writeFile.setExecutable(true)

        writeFile
    }

    private def getStream() {
        getClass().getResourceAsStream("/scripts/gitssh.sh")
    }

    private def getFile(InputStream inStream, File file) {

        if (inStream == null || file == null) {
            return null
        }

        OutputStream outStream = new FileOutputStream(file)

        byte[] buffer = new byte[1024]

        int length

        while ((length = inStream.read(buffer)) > 0) {
            outStream.write(buffer, 0, length)
        }

        outStream.close()
    }
}
