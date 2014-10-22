package org.arquillian.spacelift.gradle.git

import java.io.File
import java.util.Collection;
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
 * By default it commits to "origin master". You can override this by {@link #remote(String) and {@link #branch(String)}.
 * methods.
 * 
 * @author <a href="mailto:smikloso@redhat.com">Stefan Miklosovic</a>
 * 
 */
class GitPushTool extends Tool<File, File> {

    private Logger logger = Logger.getLogger(GitPushTool.class.getName())

    private String remote = "origin"

    private String branch = "master"

    @Override
    protected Collection<String> aliases() {
        ["git_push"]
    }

    /**
     * By default set to "origin".
     * 
     * @param remote
     * @return
     */
    public GitPushTool remote(String remote) {
        this.remote = remote;
        this
    }

    /**
     * By default set to "master"
     * 
     * @param branch
     * @return
     */
    public GitPushTool branch(String branch) {
        this.branch = branch;
        this
    }

    @Override
    protected File process(File repositoryDir) throws Exception {

        Command command = new CommandBuilder("git")
                .parameter("push")
                .parameter(remote)
                .parameter(branch)
                .build();

        ProcessResult result = null

        logger.info(command.toString())

        try {
            result = Tasks.prepare(CommandTool).workingDir(repositoryDir.getAbsolutePath()).command(command).execute().await()
        } catch (ExecutionException ex) {
            if (result != null) {
                throw new ExecutionException(
                String.format("Command %s exitted with value %s.", command.toString(), result.exitValue()),
                ex.getMessage())
            }
        }

        repositoryDir
    }
}
