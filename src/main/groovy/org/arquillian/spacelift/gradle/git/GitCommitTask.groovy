package org.arquillian.spacelift.gradle.git

import java.io.File
import java.util.Date
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
class GitCommitTask extends Task<File, File> {

    private Logger logger = Logger.getLogger(GitCommitTask.class.getName())

    private String message = "<unknown>"

    /**
     * 
     * @param message commit message, by default current date
     * @return
     */
    GitCommitTask message(String message) {
        this.message = message
        this
    }

    @Override
    protected File process(File repositoryDir) throws Exception {

        Command command = new CommandBuilder("git")
                .parameter("--git-dir=" + repositoryDir.getAbsolutePath() + System.getProperty("file.separator") + ".git")
                .parameter("--work-tree=" + repositoryDir.getAbsolutePath())
                .parameter("commit")
                .parameter("-a")
                .parameter("-m")
                .parameter(message)
                .build()

        logger.info(command.toString())

        ProcessResult result = null

        try {
            result = Tasks.prepare(CommandTool.class).command(command).execute().await()
        } catch (ExecutionException ex) {
            if (result != null) {
                throw new ExecutionException(
                String.format("Committing changes in repository %s was not successful. Command '%s', exit code: %s",
                repositoryDir.getAbsolutePath(), command.toString(), result.exitValue()),
                ex)
            }
        }

        repositoryDir
    }
}
