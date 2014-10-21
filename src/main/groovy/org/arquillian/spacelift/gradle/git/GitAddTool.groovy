package org.arquillian.spacelift.gradle.git

import java.io.File
import java.util.ArrayList
import java.util.Collection
import java.util.List
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
 * Adds files to repository.
 * 
 * @author <a href="mailto:smikloso@redhat.com">Stefan Miklosovic</a>
 * 
 */
class GitAddTool extends Tool<File, File> {

    private Logger logger = Logger.getLogger(GitAddTool.class.getName())

    private List<File> addings = new ArrayList<File>()

    @Override
    protected Collection<String> aliases() {
        ["git_add"]
    }

    /**
     * Resource to add. Null value and non existing file will be skipped from adding.
     * 
     * @param file file to add
     * @return
     */
    GitAddTool add(File file) {

        if (notNullAndExists(file)) {
            addings.add(file)
        }

        this
    }

    /**
     * Resources to add. Null values and non existing files will be skipped from adding.
     * 
     * @param files files to add
     * @return
     */
    GitAddTool add(List<File> files) {

        for (File f : files) {
            add(f)
        }

        this
    }

    @Override
    protected File process(File repositoryDir) throws Exception {

        // nothing to add
        if (addings.isEmpty()) {
            addings.add(repositoryDir) // mimics 'git add .'
        }

        CommandBuilder commandBuilder = new CommandBuilder("git").parameter("add")

        for (final File file : addings) {

            File fileToAdd = null

            if (!file.isAbsolute()) {
                File f = new File(repositoryDir.getAbsolutePath(), file.getPath())
                if (f.getCanonicalPath().startsWith(repositoryDir.getAbsolutePath())) {
                    fileToAdd = f
                }
            } else {
                if (file.getCanonicalPath().startsWith(repositoryDir.getAbsolutePath())) {
                    fileToAdd = file
                }
            }

            if (notNullAndExists(fileToAdd)) {
                commandBuilder.parameter(fileToAdd.getCanonicalPath())
            }
        }

        ProcessResult result = null

        Command command = commandBuilder.build()

        logger.info(command.toString())

        try {
            result = Tasks.prepare(CommandTool).workingDir(repositoryDir.getAbsolutePath()).command(command).execute().await()
        } catch (ExecutionException ex) {
            if (result) {
                throw new ExecutionException(
                String.format("Unable to add file to repository, command: %s, exit code: %s", command.toString(), result.exitValue()),
                ex.getMessage())
            }
        }

        repositoryDir
    }

    private boolean notNullAndExists(File value) {
        value && value.exists()
    }

}
