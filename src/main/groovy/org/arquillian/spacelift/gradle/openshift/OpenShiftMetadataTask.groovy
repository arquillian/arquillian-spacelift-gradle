package org.arquillian.spacelift.gradle.openshift

import org.arquillian.spacelift.process.ProcessResult
import org.arquillian.spacelift.task.Task

/**
 *
 * @author <a href="mailto:smikloso@redhat.com">Stefan Miklosovic</a>
 *
 */
class OpenShiftMetadataTask extends Task<ProcessResult, OpenShiftMetadata> {

    private ProcessResult processResult

    @Override
    protected OpenShiftMetadata process(ProcessResult processResult) throws Exception {

        OpenShiftMetadata metadata = new OpenShiftMetadata()

        if (!processResult || processResult.output().isEmpty()) {
            return metadata
        }

        this.processResult = processResult

        metadata.setUrl(get("URL:"))
        metadata.setSsh(get("SSH to:"))
        metadata.setGit(get("Git remote:"))
        metadata.setCloned(get("Cloned to:"))
        metadata.setDBUser(get("Root User:"))
        metadata.setDBPassword(get("Root Password:"))
        metadata.setDBName(get("Database Name:"))

        metadata
    }

    private get(String what) {
        String result

        for (String line : processResult.output()) {
            if (line.contains(what)) {
                String[] parse = line.split(": ")

                if (parse.length == 2) {
                    result = parse[1].trim()
                }
                break
            }
        }

        result
    }
}
