package org.arquillian.spacelift.gradle.git

import java.util.logging.Logger

import org.arquillian.spacelift.Spacelift
import org.arquillian.spacelift.execution.ExecutionException
import org.arquillian.spacelift.process.Command
import org.arquillian.spacelift.process.CommandBuilder
import org.arquillian.spacelift.task.Task
import org.arquillian.spacelift.task.os.CommandTool

/**
 * Creates a branch and sets it as a tracking branch, by default to branch 'master'. When a branch to create
 * is not set by method {@link #branch(String)}, processing of this tool does effectively nothing with repository.
 *
 * @author <a href="mailto:smikloso@redhat.com">Stefan Miklosovic</a>
 *
 */
class GitBranchTool extends Task<File, File> {

    private Logger logger = Logger.getLogger(GitBranchTool.class.getName())

    private String branch = null

    private String trackingBranch = "master"

    /**
     * Creates a branch.
     *
     * @param branch , branch to create, null value and empty string will not be taken into consideration
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
     * @param trackingBranch , null value and empty string will be not be taken into consideration
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

        logger.info(command.toString())

        try {
            Spacelift.task(CommandTool).workingDirectory(repositoryDir).command(command).execute().await()
        } catch (ExecutionException ex) {
            throw new ExecutionException(
                    ex, "Creating branch '{0}' in repository '{1}' was not successful. Command '{2}'.", branch,
                    repositoryDir.getAbsolutePath(), command.toString())
        }

        repositoryDir
    }

    private boolean notNullAndNotEmpty(String value) {
        value && !value.isEmpty()
    }
}
