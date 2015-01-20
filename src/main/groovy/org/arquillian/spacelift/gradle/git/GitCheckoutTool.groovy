package org.arquillian.spacelift.gradle.git

import org.arquillian.spacelift.execution.ExecutionException
import org.arquillian.spacelift.execution.Tasks
import org.arquillian.spacelift.process.Command
import org.arquillian.spacelift.process.CommandBuilder
import org.arquillian.spacelift.process.impl.CommandTool
import org.arquillian.spacelift.tool.Tool

import java.util.logging.Logger

/**
 * Checkouts some branch. If {@link #checkout(String)} is not called, it is checked out to master.
 *
 * @author <a href="mailto:smikloso@redhat.com">Stefan Miklosovic</a>
 *
 */
class GitCheckoutTool extends Tool<File, File> {

    private Logger logger = Logger.getLogger(GitCheckoutTool.class.getName())

    private String branch = "master"

    @Override
    protected Collection<String> aliases() {
        ["git_checkout"]
    }

    /**
     *
     * @param branch , branch to check out, it is skipped when it is null object or it is empty string.
     * @return
     */
    GitCheckoutTool checkout(String branch) {
        if (notNullAndNotEmpty(branch)) {
            this.branch = branch
        }
        this
    }

    @Override
    protected File process(File repositoryDir) throws Exception {

        Command command = new CommandBuilder("git")
                .parameter("checkout")
                .parameter(branch).build()

        logger.info(command.toString())

        try {
            Tasks.prepare(CommandTool).workingDirectory(repositoryDir).command(command).execute().await()
        } catch (ExecutionException ex) {
            throw new ExecutionException(
                    ex, "Checking out branch '{0}' in repository '{1}' was not successful. Command '{2}'.", branch,
                    repositoryDir.getAbsolutePath(), command.toString())
        }

        repositoryDir
    }

    private boolean notNullAndNotEmpty(String value) {
        value && !value.isEmpty()
    }
}
