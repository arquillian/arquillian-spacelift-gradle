package org.arquillian.spacelift.gradle.git

import java.io.File
import java.util.Collection;
import java.util.logging.Logger;

import org.arquillian.spacelift.execution.ExecutionException
import org.arquillian.spacelift.execution.Tasks;
import org.arquillian.spacelift.process.Command
import org.arquillian.spacelift.process.CommandBuilder
import org.arquillian.spacelift.process.ProcessResult
import org.arquillian.spacelift.process.impl.CommandTool;
import org.arquillian.spacelift.tool.Tool;

/**
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

        ProcessResult result = null

        try {
            result = Tasks.prepare(CommandTool).command(command).execute().await()
        } catch (ExecutionException ex) {
            if (result != null) {
                throw new ExecutionException(
                String.format("Unable to initialize repository at %s", repositoryDir.getAbsolutePath()),
                ex.getMessage())
            }
        }

        repositoryDir.absoluteFile.toURI()
    }
}
