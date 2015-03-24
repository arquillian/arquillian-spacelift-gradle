package org.arquillian.spacelift.gradle.git

import org.arquillian.spacelift.Spacelift
import org.arquillian.spacelift.execution.ExecutionException
import org.arquillian.spacelift.process.CommandBuilder
import org.arquillian.spacelift.task.Task
import org.arquillian.spacelift.task.os.CommandTool

/**
 * Finds SHA1 of a given reference
 * @author kpiwko
 *
 */
class GitRevParseTool extends Task<File, String> {

    String rev

    GitRevParseTool rev(String rev) {
        this.rev = rev
        return this
    }

    @Override
    protected String process(File repositoryDir) throws Exception {

        CommandBuilder command = new CommandBuilder("git")
                .parameter("rev-parse")
                .parameter(rev)

        try {
            List<String> output = Spacelift.task(CommandTool).workingDirectory(repositoryDir).shouldExitWith(0,128).command(command).execute().await().output()
            if(output!=null && output.size()==1) {
                return output.get(0);
            }
            // no such sha was found
            return null
        }
        catch(ExecutionException ex) {
            throw new ExecutionException(ex, "Unable to get rev sha of '{0}' at '{1}'. Command '{2}.", rev, repositoryDir.getCanonicalPath(), command.toString())
        }
    }
}
