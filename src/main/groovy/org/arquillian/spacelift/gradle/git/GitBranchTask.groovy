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
 * 
 * @author <a href="mailto:smikloso@redhat.com">Stefan Miklosovic</a>
 * 
 */
class GitBranchTask extends Task<File, File> {

    private Logger logger = Logger.getLogger(GitBranchTask.class.getName())

    private String branch = null

    private String trackingBranch = "origin/master"

    /**
     * 
     * @param branch, branch to create, it is skipped when it is null object or it is empty
     * @return
     */
    GitBranchTask branch(String branch) {
        if (branch != null && !branch.isEmpty()) {
            this.branch = branch
        }
        this
    }

    /**
     * 
     * @param trackingBranch, by default origin/master
     * @return
     */
    GitBranchTask trackingBranch(String trackingBranch) {
        if (trackingBranch && !trackingBranch.isEmpty()) {
            this.trackingBranch = trackingBranch
        }
        this
    }

    @Override
    protected File process(File repositoryDir) throws Exception {

        // no branch to create
        if (!branch) {
            return repositoryDir
        }

        Command command = new CommandBuilder("git")
                .parameter("--git-dir=" + repositoryDir.getAbsolutePath() + System.getProperty("file.separator") + ".git")
                .parameter("--work-tree=" + repositoryDir.getAbsolutePath())
                .parameter("branch")
                .parameter("--track")
                .parameter(branch)
                .parameter(trackingBranch)
                .build()

        ProcessResult result = null

        logger.info(command.toString())

        try {
            result = Tasks.prepare(CommandTool.class).command(command).execute().await()
        } catch (ExecutionException ex) {
            if (result != null) {
                throw new ExecutionException(
                String.format("Creating branch '%s' in repository %s was not successful. Command '%s', exit code: %s",
                branch, repositoryDir.getAbsolutePath(), command.toString(), result.exitValue()),
                ex)
            }
        }

        repositoryDir
    }

}
