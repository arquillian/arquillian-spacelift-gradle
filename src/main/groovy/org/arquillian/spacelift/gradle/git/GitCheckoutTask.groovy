package org.arquillian.spacelift.gradle.git

import java.io.File
import java.util.logging.Logger

import org.arquillian.spacelift.execution.ExecutionException
import org.arquillian.spacelift.execution.Task
import org.arquillian.spacelift.execution.Tasks
import org.arquillian.spacelift.process.Command
import org.arquillian.spacelift.process.CommandBuilder
import org.arquillian.spacelift.process.ProcessResult
import org.arquillian.spacelift.process.impl.CommandTool

/**
 * Checkout some branch. If {@link #checkout(String)} is not called, it is checked out to master.
 * 
 * @author <a href="mailto:smikloso@redhat.com">Stefan Miklosovic</a>
 * 
 */
class GitCheckoutTask extends Task<File, File> {

    private Logger logger = Logger.getLogger(GitCheckoutTask.class.getName())

    private String branch = "master"

    /**
     * 
     * @param branch, branch to check out, it is skipped when it is null object or it is empty
     * @return
     */
    GitCheckoutTask checkout(String branch) {
        if (branch && !branch.isEmpty()) {
            this.branch = branch
        }
        this
    }

    @Override
    protected File process(File repositoryDir) throws Exception {

        Command command = new CommandBuilder("git")
                .parameter("--git-dir=" + repositoryDir.getAbsolutePath() + System.getProperty("file.separator") + ".git")
                .parameter("--work-tree=" + repositoryDir.getAbsolutePath())
                .parameter("checkout")
                .parameter(branch).build()

        ProcessResult result = null

        logger.info(command.toString())

        try {
            result = Tasks.prepare(CommandTool.class).command(command).execute().await()
        } catch (ExecutionException ex) {
            if (result != null) {
                throw new ExecutionException(
                String.format("Checking out branch '%s' in repository %s was not successful. Command '%s', exit code: %s",
                branch, repositoryDir.getAbsolutePath(), command.toString(), result.exitValue()),
                ex)
            }
        }

        repositoryDir
    }
}
