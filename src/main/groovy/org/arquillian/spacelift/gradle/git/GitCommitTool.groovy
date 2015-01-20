package org.arquillian.spacelift.gradle.git

import org.arquillian.spacelift.execution.ExecutionException
import org.arquillian.spacelift.execution.Tasks
import org.arquillian.spacelift.process.Command
import org.arquillian.spacelift.process.CommandBuilder
import org.arquillian.spacelift.process.impl.CommandTool
import org.arquillian.spacelift.tool.Tool

import java.util.logging.Logger

/**
 * Commits changes to repository.
 *
 * @author <a href="mailto:smikloso@redhat.com">Stefan Miklosovic</a>
 *
 */
class GitCommitTool extends Tool<File, File> {

    private Logger logger = Logger.getLogger(GitCommitTool.class.getName())

    private String message = "<unknown>"

    @Override
    protected Collection<String> aliases() {
        ["git_commit"]
    }

    /**
     *
     * @param message commit message, by default '{@literal <unknown>}'. Null values and empty strings are not taken into consideration.
     * @return
     */
    GitCommitTool message(String message) {
        if (notNullAndNotEmpty(message)) {
            this.message = message
        }
        this
    }

    @Override
    protected File process(File repositoryDir) throws Exception {

        Command command = new CommandBuilder("git")
                .parameter("commit")
                .parameter("-m")
                .parameter(message)
                .build()

        logger.info(command.toString())

        try {
            Tasks.prepare(CommandTool).workingDirectory(repositoryDir).command(command).execute().await()
        } catch (ExecutionException ex) {
            throw new ExecutionException(
                    ex, "Committing changes in repository '{0}' was not successful. Command '{1}'.",
                    repositoryDir.getAbsolutePath(), command.toString())

        }

        repositoryDir
    }

    private boolean notNullAndNotEmpty(String value) {
        value && !value.isEmpty()
    }
}
