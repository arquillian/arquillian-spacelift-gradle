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
 * By default it pushes to "{@literal origin :.}". You can override this by {@link #remote(String) and {@link #branch(String)}.
 * methods.
 * 
 * @author <a href="mailto:smikloso@redhat.com">Stefan Miklosovic</a>
 * 
 */
class GitPushTool extends Tool<File, File> {

    private Logger logger = Logger.getLogger(GitPushTool.class.getName())

    private String remote = "origin"

    private String branch = ":"

    private boolean tags

    private File gitSsh
    
    @Override
    protected Collection<String> aliases() {
        ["git_push"]
    }

    /**
     * By default set to "origin". Null values and empty strings are not taken into consideration.
     * 
     * @param remote
     * @return
     */
    GitPushTool remote(String remote) {
        if (notNullAndNotEmpty(remote)) {
            this.remote = remote
        }
        this
    }

    /**
     * By default set to ":". Null values and empty strings are not taken into consideration.
     * 
     * @param branch
     * @return
     */
    GitPushTool branch(String branch) {
        if (notNullAndNotEmpty(branch)) {
            this.branch = branch
        }
        this
    }

    /**
     * Pushes tags as well.
     * 
     * @return 
     */
    GitPushTool tags() {
        tags = true
        this
    }

    /**
     *
     * @param gitSsh file to use as GIT_SSH script, skipped when it does not exist, it is not a file or is a null object
     * @return
     */
    GitPushTool gitSsh(File gitSsh) {
        if (gitSsh && gitSsh.exists() && gitSsh.isFile()) {
            this.gitSsh = gitSsh
        }
        this
    }
    
    @Override
    protected File process(File repositoryDir) throws Exception {

        CommandBuilder commandBuilder = new CommandBuilder("git").parameter("push")

        if (tags) {
            commandBuilder.parameter("--tags")
        }

        Command command = commandBuilder.parameter(remote).parameter(branch).build()

        ProcessResult result = null

        logger.info(command.toString())

        try {
            CommandTool push = Tasks.prepare(CommandTool).workingDir(repositoryDir.getAbsolutePath()).command(command)
            
            if (gitSsh) {
                push.addEnvironment(["GIT_SSH": gitSsh.getAbsolutePath()])
            }
            
            result = push.execute().await()
        } catch (ExecutionException ex) {
            if (result != null) {
                throw new ExecutionException(
                String.format("Command %s exitted with value %s.", command.toString(), result.exitValue()),
                ex.getMessage())
            }
        }

        repositoryDir
    }

    private boolean notNullAndNotEmpty(String value) {
        value && !value.isEmpty()
    }
}
