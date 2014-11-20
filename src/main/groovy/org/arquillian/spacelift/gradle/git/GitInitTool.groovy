package org.arquillian.spacelift.gradle.git

import org.arquillian.spacelift.execution.ExecutionException
import org.arquillian.spacelift.execution.Tasks
import org.arquillian.spacelift.process.Command
import org.arquillian.spacelift.process.CommandBuilder
import org.arquillian.spacelift.process.impl.CommandTool
import org.arquillian.spacelift.tool.Tool

import java.util.logging.Logger

/**
 * Initializes repository. Initialized repository is bare.
 *
 * @author <a href="mailto:smikloso@redhat.com">Stefan Miklosovic</a>
 *
 */
class GitInitTool extends Tool<File, URI> {

    private Logger logger = Logger.getLogger(GitInitTool.class.getName())

    @Override
    protected Collection<String> aliases() {
        ["git_init"]
    }

    @Override
    protected URI process(File repositoryDir) throws Exception {

        Command command = new CommandBuilder("git").parameters("init", "--bare", repositoryDir.getAbsolutePath()).build()

        logger.info(command.toString())

        try {
            Tasks.prepare(CommandTool).command(command).execute().await()
        } catch (ExecutionException ex) {
            throw new ExecutionException(ex, "Unable to initialize repository at {0}", repositoryDir.getAbsolutePath())
        }

        repositoryDir.absoluteFile.toURI()
    }
}
