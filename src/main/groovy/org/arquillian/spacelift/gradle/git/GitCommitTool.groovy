package org.arquillian.spacelift.gradle.git

import java.io.File
import java.util.Collection;
import java.util.Date
import java.util.logging.Logger

import org.arquillian.spacelift.execution.ExecutionException
import org.arquillian.spacelift.execution.Task
import org.arquillian.spacelift.execution.Tasks
import org.arquillian.spacelift.process.Command
import org.arquillian.spacelift.process.CommandBuilder
import org.arquillian.spacelift.process.ProcessResult
import org.arquillian.spacelift.process.impl.CommandTool
import org.arquillian.spacelift.tool.Tool;

/**
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
     * @param message commit message, by default current date
     * @return
     */
    GitCommitTool message(String message) {
        this.message = message
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

        ProcessResult result = null

        try {
            result = Tasks.prepare(CommandTool).workingDir(repositoryDir.getAbsolutePath()).command(command).execute().await()
        } catch (ExecutionException ex) {
            if (result != null) {
                throw new ExecutionException(
                String.format("Committing changes in repository '%s' was not successful. Command '%s', exit code: %s",
                repositoryDir.getAbsolutePath(), command.toString(), result.exitValue()),
                ex)
            }
        }

        repositoryDir
    }
}
