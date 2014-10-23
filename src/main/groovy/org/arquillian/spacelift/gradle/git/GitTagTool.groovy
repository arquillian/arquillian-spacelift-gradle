package org.arquillian.spacelift.gradle.git

import java.io.File
import java.util.Collection
import java.util.logging.Logger

import org.arquillian.spacelift.execution.ExecutionException
import org.arquillian.spacelift.execution.Tasks
import org.arquillian.spacelift.process.Command
import org.arquillian.spacelift.process.CommandBuilder
import org.arquillian.spacelift.process.ProcessResult
import org.arquillian.spacelift.process.impl.CommandTool
import org.arquillian.spacelift.tool.Tool

/**
 * Tags repository with {@link #tag(String)}. You can tag particular commit with {@link #commit(String)}. Deletion of tag 
 * is done by {@link #delete}, forcing by {@link #force} flags. You can not use force and delete flags together.
 * 
 * @author <a href="mailto:smikloso@redhat.com">Stefan Miklosovic</a>
 *
 */
class GitTagTool extends Tool<File, File> {

    private Logger logger = Logger.getLogger(GitTagTool.class.getName())

    private boolean delete

    private boolean force

    private String commit

    private String tag = "unknown tag"

    @Override
    protected Collection<String> aliases() {
        ["git_tag"]
    }

    GitTagTool delete() {
        delete = true
        this
    }

    GitTagTool force() {
        force = true
        this
    }

    /**
     * 
     * @param commit hash of commit you want to tag, null values and empty strings are not taken into consideration.
     * @return
     */
    GitTagTool commit(String commit) {
        if (notNullAndExists(commit)) {
            this.commit = commit
        }
        this
    }

    GitTagTool tag(String tag) {
        this.tag = tag
        this
    }

    @Override
    protected File process(File repositoryDir) throws Exception {

        CommandBuilder commandBuilder = new CommandBuilder("git").parameter("tag")

        if (delete) {
            commandBuilder.parameter("-d")
        }

        if (force && !delete) {
            commandBuilder.parameter("-f")
        }

        commandBuilder.parameter(tag)

        if (commit && !delete) {
            commandBuilder.parameter(commit)
        }

        Command command = commandBuilder.build()

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
    
    private boolean notNullAndExists(File value) {
        value && value.exists()
    }
}
