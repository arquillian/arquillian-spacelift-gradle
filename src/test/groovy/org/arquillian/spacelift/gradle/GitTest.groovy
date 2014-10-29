package org.arquillian.spacelift.gradle

import static org.junit.Assert.*
import static org.hamcrest.CoreMatchers.*

import java.io.File
import java.io.IOException

import org.arquillian.spacelift.execution.Tasks
import org.arquillian.spacelift.execution.impl.DefaultExecutionServiceFactory
import org.arquillian.spacelift.gradle.git.GitAddTool
import org.arquillian.spacelift.gradle.git.GitBranchTool
import org.arquillian.spacelift.gradle.git.GitCheckoutTool
import org.arquillian.spacelift.gradle.git.GitCloneTool
import org.arquillian.spacelift.gradle.git.GitCommitTool
import org.arquillian.spacelift.gradle.git.GitFetchTool
import org.arquillian.spacelift.gradle.git.GitInitTool
import org.arquillian.spacelift.gradle.git.GitPushTool
import org.arquillian.spacelift.gradle.git.GitRemoveTool
import org.arquillian.spacelift.gradle.git.GitSshFileTask
import org.arquillian.spacelift.gradle.git.GitTagTool
import org.hamcrest.core.Is
import org.junit.Assert
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * 
 * 
 * @author <a href="mailto:smikloso@redhat.com">Stefan Miklosovic</a>
 * 
 */
class GitTest {

    @BeforeClass
    static void setup() {
        Tasks.setDefaultExecutionServiceFactory(new DefaultExecutionServiceFactory())
    }

    @Test
    void initAndBranchGitTest() {

        File repositoryInitDir = new File(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString())

        File repositoryCloneDir = new File(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString())

        Tasks.chain(repositoryInitDir, GitInitTool).then(GitCloneTool).destination(repositoryCloneDir).execute().await()

        File dummyFile1 = new File(repositoryCloneDir, "dummyFile")
        File dummyFile2 = new File(repositoryCloneDir, "dummyFile2")

        dummyFile1.createNewFile()
        dummyFile2.createNewFile()

        // this will not be added
        File outsideOfRepository = new File(repositoryCloneDir.getParentFile(), UUID.randomUUID().toString())
        outsideOfRepository.createNewFile()

        File outsideOfRepositoryRelative = new File(repositoryCloneDir, "../" + UUID.randomUUID().toString())
        outsideOfRepositoryRelative.createNewFile()

        Tasks.chain(repositoryCloneDir, GitAddTool).add([dummyFile1,dummyFile2,outsideOfRepository])
            .then(GitCommitTool).message("added some files")
            .then(GitPushTool)
            .then(GitBranchTool.class).branch("dummyBranch")
            .then(GitCheckoutTool.class).checkout("dummyBranch")
            .execute().await()

        File dummyFile3 = new File(repositoryCloneDir, "dummyFile3")
        File dummyFile4 = new File(repositoryCloneDir, "dummyFile4")

        dummyFile3.createNewFile()
        dummyFile4.createNewFile()

        Tasks.chain(repositoryCloneDir, GitAddTool).add([dummyFile3, dummyFile4, outsideOfRepositoryRelative])
            .then(GitRemoveTool).force().remove(dummyFile3).remove([outsideOfRepository, outsideOfRepositoryRelative])
            .then(GitCommitTool).message("deleted dummyFile3 and added dummyFile4")
            .then(GitTagTool).tag("1.0.0")
            .then(GitPushTool).tags().branch("dummyBranch")
            .execute().await()

        File repository2CloneDir = new File(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString())

        Tasks.chain(repositoryInitDir.toURI(), GitCloneTool).destination(repository2CloneDir)
            .then(GitFetchTool)
            .then(GitCheckoutTool).checkout("dummyBranch")
            .execute().await()

        Assert.assertFalse(dummyFile3.exists())
        Assert.assertTrue(dummyFile4.exists())
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

        File gitSshFile = Tasks.prepare(GitSshFileTask).execute().await()

        Tasks.chain(new URI("ssh://git@github.com/smiklosovic/test.git"), GitCloneTool).gitSsh(gitSshFile).destination(repositoryCloneDir)
            .then(GitFetchTool).gitSsh(gitSshFile)
            .then(GitBranchTool).branch("dummyBranch")
            .then(GitCheckoutTool).checkout("dummyBranch")
            .execute().await()

        File file1 = new File(repositoryCloneDir, UUID.randomUUID().toString())
        File file2 = new File(repositoryCloneDir, UUID.randomUUID().toString())

        file1.createNewFile()
        file2.createNewFile()

        Tasks.chain(repositoryCloneDir, GitAddTool).add([file1, file2])
            .then(GitCommitTool).message("added some files")
            .then(GitPushTool).gitSsh(gitSshFile).remote("origin").branch("dummyBranch")
            .then(GitPushTool).gitSsh(gitSshFile).remote("origin").branch(":dummyBranch") // delete that branch
            .execute().await()
    }
}
