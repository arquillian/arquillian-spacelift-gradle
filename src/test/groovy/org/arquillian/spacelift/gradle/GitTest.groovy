package org.arquillian.spacelift.gradle

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

import org.arquillian.spacelift.Spacelift
import org.arquillian.spacelift.gradle.git.*
import org.arquillian.spacelift.task.os.CommandTool
import org.junit.Ignore
import org.junit.Test

/**
 *
 *
 * @author <a href="mailto:smikloso@redhat.com">Stefan Miklosovic</a>
 *
 */
class GitTest {

    private static final URI testRepository = new URI("ssh://git@github.com/smiklosovic/test.git")

    @Test
    void initAndBranchGitTest() {

        File repositoryInitDir = new File(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString())

        File repositoryCloneDir = new File(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString())

        Spacelift.task(repositoryInitDir, GitInitTool).then(GitCloneTool).destination(repositoryCloneDir).execute().await()

        // We need to configure the user, otherwise it git will be failing with exit code 128
        Spacelift.task(CommandTool)
                .workingDir(repositoryCloneDir.absolutePath)
                .programName('git')
                .parameters('config', 'user.email', 'spacelift@arquillian.org').execute().await()

        Spacelift.task(CommandTool)
                .workingDir(repositoryCloneDir.absolutePath)
                .programName('git')
                .parameters('config', 'user.name', 'Arquillian Spacelift')
                .execute().await()

        File dummyFile1 = new File(repositoryCloneDir, "dummyFile")
        File dummyFile2 = new File(repositoryCloneDir, "dummyFile2")

        dummyFile1.createNewFile()
        dummyFile2.createNewFile()

        // this will not be added
        File outsideOfRepository = new File(repositoryCloneDir.getParentFile(), UUID.randomUUID().toString())
        outsideOfRepository.createNewFile()

        File outsideOfRepositoryRelative = new File(repositoryCloneDir, "../" + UUID.randomUUID().toString())
        outsideOfRepositoryRelative.createNewFile()

        Spacelift.task(repositoryCloneDir, GitAddTool).add([dummyFile1, dummyFile2, outsideOfRepository])
                .then(GitCommitTool).message("added some files")
                .then(GitPushTool).branch("master")
                .then(GitBranchTool.class).branch("dummyBranch")
                .then(GitCheckoutTool.class).checkout("dummyBranch")
                .execute().await()


        File dummyFile3 = new File(repositoryCloneDir, "dummyFile3")
        File dummyFile4 = new File(repositoryCloneDir, "dummyFile4")

        dummyFile3.createNewFile()
        dummyFile4.createNewFile()

        Spacelift.task(repositoryCloneDir, GitAddTool).add([dummyFile3, dummyFile4, outsideOfRepositoryRelative])
                .then(GitRemoveTool).force().remove(dummyFile3).remove([outsideOfRepository, outsideOfRepositoryRelative])
                .then(GitCommitTool).message("deleted dummyFile3 and added dummyFile4")
                .then(GitTagTool).tag("1.0.0")
                .then(GitPushTool).tags().branch("dummyBranch")
                .execute().await()

        File repository2CloneDir = new File(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString())

        Spacelift.task(repositoryInitDir.toURI(), GitCloneTool).destination(repository2CloneDir)
                .then(GitFetchTool)
                .then(GitCheckoutTool).checkout("dummyBranch")
                .execute().await()

        assertFalse(dummyFile3.exists())
        assertTrue(dummyFile4.exists())
    }

    /**
     * Ignored since it needs specific repository to clone from and push to which is not guaranteed always exists.
     *
     * @throws IOException
     */
    @Test
    @Ignore
    public void testGit() throws IOException {

        File repositoryCloneDir = new File(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString())

        File gitSshFile = Spacelift.task(GitSshFileTask).execute().await()

        Spacelift.task(testRepository, GitCloneTool).gitSsh(gitSshFile).destination(repositoryCloneDir)
                .then(GitFetchTool)
                .then(GitBranchTool).branch("dummyBranch")
                .then(GitCheckoutTool).checkout("dummyBranch")
                .execute().await()

        File file1 = new File(repositoryCloneDir, UUID.randomUUID().toString())
        File file2 = new File(repositoryCloneDir, UUID.randomUUID().toString())

        file1.createNewFile()
        file2.createNewFile()

        Spacelift.task(repositoryCloneDir, GitAddTool).add([file1, file2])
                .then(GitCommitTool).message("added some files")
                .then(GitPushTool).remote("origin").branch("dummyBranch")
                .then(GitPushTool).remote("origin").branch(":dummyBranch") // delete that branch
                .execute().await()
    }
}
