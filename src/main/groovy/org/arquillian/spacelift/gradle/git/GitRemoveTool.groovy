package org.arquillian.spacelift.gradle.git

import java.io.File
import java.util.Collection
import java.util.logging.Logger

import org.arquillian.spacelift.execution.ExecutionException
import org.arquillian.spacelift.execution.Tasks
import org.arquillian.spacelift.process.Command
import org.arquillian.spacelift.process.CommandBuilder
import org.arquillian.spacelift.process.ProcessInteractionBuilder
import org.arquillian.spacelift.process.ProcessResult
import org.arquillian.spacelift.process.impl.CommandTool
import org.arquillian.spacelift.tool.Tool

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

        for (final File file : toRemove) {

            File fileToRemove = null

            if (!file.isAbsolute()) {
                File f = new File(repositoryDir.getCanonicalPath(), file.getPath())
                if (f.getCanonicalPath().startsWith(repositoryDir.getCanonicalPath())) {
                    fileToRemove = f
                }
            } else {
                if (file.getCanonicalPath().startsWith(repositoryDir.getCanonicalPath())) {
                    fileToRemove = file
                }
            }

            if (notNullAndExists(fileToRemove)) {
                commandBuilder.parameter(fileToRemove.getCanonicalPath())
            }
        }

        Command command = commandBuilder.build()

        logger.info(command.toString())

        ProcessResult result = null

        try {
            result = Tasks.prepare(CommandTool).workingDir(repositoryDir.getCanonicalPath()).command(command).execute().await()
        } catch (ExecutionException ex) {
            if (result != null) {
                throw new ExecutionException(
                String.format("Command %s exitted with value %s.", command.toString(), result.exitValue()),
                ex.getMessage())
            }
        }

        return repositoryDir
    }

    private boolean notNullAndExists(File value) {
        value && value.exists()
    }
}
