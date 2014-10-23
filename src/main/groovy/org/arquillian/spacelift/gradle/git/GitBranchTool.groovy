package org.arquillian.spacelift.gradle.git

import java.io.File
import java.util.Collection
import java.util.logging.Logger

import org.arquillian.spacelift.execution.ExecutionException
import org.arquillian.spacelift.execution.Task
import org.arquillian.spacelift.execution.Tasks
import org.arquillian.spacelift.process.Command
import org.arquillian.spacelift.process.CommandBuilder
import org.arquillian.spacelift.process.ProcessResult
import org.arquillian.spacelift.process.impl.CommandTool
import org.arquillian.spacelift.tool.Tool

/**
 * Creates a branch and sets it as a tracking branch, by default to branch 'master'. When a branch to create 
 * is not set by method {@link #branch(String)}, processing of this tool does effectively nothing with repository.
 *
 * @author <a href="mailto:smikloso@redhat.com">Stefan Miklosovic</a>
 * 
 */
class GitBranchTool extends Tool<File, File> {

    private Logger logger = Logger.getLogger(GitBranchTool.class.getName())

    private String branch = null

    private String trackingBranch = "master"

    @Override
    protected Collection<String> aliases() {
        ["git_branch"]
    }

    /**
     * Creates a branch.
     * 
     * @param branch, branch to create, null value and empty string will not be taken into consideration
     * @return
     */
    GitBranchTool branch(String branch) {
        if (notNullAndNotEmpty(branch)) {
            this.branch = branch
        }
        this
    }

    /**
     * Sets tracking branch, by default master
     * 
     * @param trackingBranch, null value and empty string will be not be taken into consideration
     * @return
     */
    GitBranchTool trackingBranch(String trackingBranch) {
        if (notNullAndNotEmpty(trackingBranch)) {
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
                .parameter("branch")
                .parameter("--track")
                .parameter(branch)
                .parameter(trackingBranch)
                .build()

        ProcessResult result = null

        logger.info(command.toString())

        try {
            result = Tasks.prepare(CommandTool).workingDir(repositoryDir.getAbsolutePath()).command(command).execute().await()
        } catch (ExecutionException ex) {
            if (result != null) {
                throw new ExecutionException(
                String.format("Creating branch '%s' in repository '%s' was not successful. Command '%s', exit code: %s",
                branch, repositoryDir.getAbsolutePath(), command.toString(), result.exitValue()),
                ex)
            }
        }

        repositoryDir
    }

    private boolean notNullAndNotEmpty(String value) {
        value && !value.isEmpty()
    }
}
