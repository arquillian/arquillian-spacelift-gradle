package org.arquillian.spacelift.gradle.git

import java.util.logging.Logger

import org.arquillian.spacelift.Spacelift
import org.arquillian.spacelift.execution.ExecutionException
import org.arquillian.spacelift.process.Command
import org.arquillian.spacelift.process.CommandBuilder
import org.arquillian.spacelift.task.Task
import org.arquillian.spacelift.task.os.CommandTool

/**
 * Initializes repository. Initialized repository is bare.
 *
 * @author <a href="mailto:smikloso@redhat.com">Stefan Miklosovic</a>
 *
 */
class GitInitTool extends Task<File, URI> {

    private Logger logger = Logger.getLogger(GitInitTool.class.getName())

    @Override
    protected URI process(File repositoryDir) throws Exception {

        Command command = new CommandBuilder("git").parameters("init", "--bare", repositoryDir.getAbsolutePath()).build()

        logger.info(command.toString())

        try {
            Spacelift.task(CommandTool).command(command).execute().await()
        } catch (ExecutionException ex) {
            throw new ExecutionException(ex, "Unable to initialize repository at {0}", repositoryDir.getAbsolutePath())
        }

        repositoryDir.absoluteFile.toURI()
    }
}
