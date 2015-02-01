package org.arquillian.spacelift.gradle.git

import java.util.logging.Logger

import org.arquillian.spacelift.Spacelift
import org.arquillian.spacelift.execution.ExecutionException
import org.arquillian.spacelift.process.Command
import org.arquillian.spacelift.process.CommandBuilder
import org.arquillian.spacelift.task.Task
import org.arquillian.spacelift.task.os.CommandTool

/**
 * Adds files to repository.
 *
 * @author <a href="mailto:smikloso@redhat.com">Stefan Miklosovic</a>
 *
 */
class GitAddTool extends Task<File, File> {

    private Logger logger = Logger.getLogger(GitAddTool.class.getName())

    private List<File> addings = new ArrayList<File>()

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

        for (File file : addings) {
            if (!file.isAbsolute()) {
                file = new File(repositoryDir, file.getPath())
            }

            // The file has to exist and its path has to start with repository path meaning the file is in the repository
            if (!notNullAndExists(file) || !file.getCanonicalPath().startsWith(repositoryDir.getCanonicalPath())) {
                logger.warning("Skipping file $file because it is not in the repository.")
                continue
            }

            commandBuilder.parameter(file.getAbsolutePath())
        }

        Command command = commandBuilder.build()

        logger.info(command.toString())

        try {
            Spacelift.task(CommandTool).workingDirectory(repositoryDir).command(command).execute().await()
        } catch (ExecutionException ex) {
            throw new ExecutionException(ex, "Unable to add file to repository, command: {0}.", command.toString())
        }

        repositoryDir
    }

    private boolean notNullAndExists(File value) {
        value && value.exists()
    }

}
