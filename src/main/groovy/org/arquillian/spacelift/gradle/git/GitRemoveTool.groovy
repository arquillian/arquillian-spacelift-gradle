package org.arquillian.spacelift.gradle.git

import org.arquillian.spacelift.execution.ExecutionException
import org.arquillian.spacelift.execution.Tasks
import org.arquillian.spacelift.process.Command
import org.arquillian.spacelift.process.CommandBuilder
import org.arquillian.spacelift.process.impl.CommandTool
import org.arquillian.spacelift.tool.Tool

import java.util.logging.Logger

/**
 * Removes files from repository.
 *
 * @author <a href="mailto:smikloso@redhat.com">Stefan Miklosovic</a>
 *
 */
class GitRemoveTool extends Tool<File, File> {

    private Logger logger = Logger.getLogger(GitRemoveTool.class.getName())

    private boolean force

    private boolean recursive

    private boolean quiet

    private List<File> toRemove = new ArrayList<File>()

    @Override
    protected Collection<String> aliases() {
        ["git_rm"]
    }

    /**
     * Turns {@plain -f} flag on.
     * @return
     */
    GitRemoveTool force() {
        force = true
        this
    }

    /**
     * Turns {@plain -r} flag on.
     *
     * @return
     */
    GitRemoveTool recursive() {
        recursive = true
        this
    }

    /**
     * Turns {@plain -q} flag on.
     *
     * @return
     */
    GitRemoveTool quiet() {
        quiet = true
        this
    }

    /**
     *
     * @param file file to remove, null values or nonexisting files are not taken into consideration
     * @return
     */
    GitRemoveTool remove(File file) {
        if (notNullAndExists(file)) {
            toRemove.add(file)
        }
        this
    }

    /**
     *
     * @param files files to remove, null values or nonexisting files are not taken into consideration
     * @return
     */
    GitRemoveTool remove(List<File> files) {
        for (File f : files) {
            remove(f)
        }
        this
    }

    @Override
    protected File process(File repositoryDir) throws Exception {

        CommandBuilder commandBuilder = new CommandBuilder("git").parameter("rm")

        if (force) {
            commandBuilder.parameter("-f")
        }

        if (recursive) {
            commandBuilder.parameter("-r")
        }

        if (quiet) {
            commandBuilder.parameter("-q")
        }

        for (File file : toRemove) {
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
            Tasks.prepare(CommandTool).workingDirectory(repositoryDir).command(command).execute().await()
        } catch (ExecutionException ex) {
            throw new ExecutionException(ex, "Could not remove files with command {0}.", command.toString())
        }

        return repositoryDir
    }

    private boolean notNullAndExists(File value) {
        value && value.exists()
    }
}
