package org.arquillian.spacelift.gradle.openshift

import org.arquillian.spacelift.execution.Task
import org.arquillian.spacelift.process.ProcessResult

/**
 * 
 * @author <a href="mailto:smikloso@redhat.com">Stefan Miklosovic</a>
 *
 */
class OpenshiftMetadataTask extends Task<ProcessResult, OpenshiftMetadata> {

    private ProcessResult processResult

    @Override
    protected OpenshiftMetadata process(ProcessResult processResult) throws Exception {

        OpenshiftMetadata metadata = new OpenshiftMetadata()

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
