package org.arquillian.spacelift.gradle.git

import java.io.File
import java.util.ArrayList
import java.util.List
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
class GitAddTask extends Task<File, File> {

    private Logger logger = Logger.getLogger(GitAddTask.class.getName())

    private List<File> addings = new ArrayList<File>()

    /**
     * Resources to add. Null values and empty strings will be skipped from adding.
     * 
     * @param file file to add
     * @param files other files to add
     * @return
     */
    GitAddTask add(File file) {

        if (notNullAndExists(file)) {
            addings.add(file)
        }

        this
    }

    GitAddTask add(List<File> files) {

        for (File f : files) {
            add(f)
        }

        this
    }

    @Override
    protected File process(File repositoryDir) throws Exception {

        // nothing to add
        if (addings.isEmpty()) {
            return repositoryDir
        }

        CommandBuilder commandBuilder = new CommandBuilder("git")
                .parameter("--git-dir=" + repositoryDir.getAbsolutePath() + System.getProperty("file.separator") + ".git")
                .parameter("--work-tree=" + repositoryDir.getAbsolutePath())
                .parameter("add")

        for (final File file : addings) {

            File fileToAdd = null

            if (!file.isAbsolute()) {
                fileToAdd = new File(repositoryDir.getAbsoluteFile(), file.getPath())
            } else {
                fileToAdd = file
            }

            commandBuilder.parameter(fileToAdd.getAbsolutePath())
        }

        ProcessResult result = null

        Command command = commandBuilder.build()

        logger.info(command.toString())

        try {
            result = Tasks.prepare(CommandTool.class).command(command).execute().await()
        } catch (ExecutionException ex) {
            if (result) {
                throw new ExecutionException(
                String.format("Unable to add file to repository, command: %s, exit code: %s", command.toString(), result.exitValue()),
                ex.getMessage())
            }
        }

        return repositoryDir
    }

    private boolean notNullAndExists(File value) {
        value && value.exists();
    }

}
