package org.arquillian.spacelift.gradle;

import java.io.File;
import java.io.IOException;

import org.arquillian.spacelift.execution.Tasks;
import org.arquillian.spacelift.execution.impl.DefaultExecutionServiceFactory;
import org.arquillian.spacelift.gradle.git.GitAddTask;
import org.arquillian.spacelift.gradle.git.GitBranchTask;
import org.arquillian.spacelift.gradle.git.GitCheckoutTask;
import org.arquillian.spacelift.gradle.git.GitCloneTask;
import org.arquillian.spacelift.gradle.git.GitCommitTask;
import org.arquillian.spacelift.gradle.git.GitPushTask;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Ignored since it needs specific repository to clone from and push to which is not guaranteed always exists.
 * 
 * @author <a href="mailto:smikloso@redhat.com">Stefan Miklosovic</a>
 * 
 */
@Ignore
public class GitTest {

    private File repositoryDir = null;

    @BeforeClass
    public static void setup() {
        Tasks.setDefaultExecutionServiceFactory(new DefaultExecutionServiceFactory());
    }

    @Before
    public void beforeTest() throws IOException {

        repositoryDir = File.createTempFile("spacelift-git-test-repo-", Long.toString(System.nanoTime()));

        if (!(repositoryDir.delete())) {
            throw new IOException("Could not delete temp file: " + repositoryDir.getAbsolutePath());
        }

        if (!(repositoryDir.mkdir())) {
            throw new IOException("Could not create temp directory: " + repositoryDir.getAbsolutePath());
        }
    }

    @Test
    public void simpleTestGit() throws IOException {

        Tasks.chain("git@github.com:smiklosovic/test.git", GitCloneTask).destination(repositoryDir).execute().await();

        File file1 = new File(repositoryDir, "dummyFile");
        File file2 = new File(repositoryDir, "dummyFile2");

        file1.createNewFile();
        file2.createNewFile();

        Tasks.chain(repositoryDir, GitAddTask).add(repositoryDir)
                .then(GitCommitTask).message("added some files")
                .then(GitPushTask.class)
                .execute().await();
    }

    @Test
    public void testGit() throws IOException {

        Tasks.chain("git@github.com:smiklosovic/test.git", GitCloneTask.class).destination(repositoryDir)
                .then(GitBranchTask.class).branch("dummyBranch")
                .then(GitCheckoutTask.class).checkout("dummyBranch").execute().await();

        File file1 = new File(repositoryDir, "dummyFile");
        File file2 = new File(repositoryDir, "dummyFile2");

        file1.createNewFile();
        file2.createNewFile();

        Tasks.chain(repositoryDir, GitAddTask.class).add(file1, file2)
                .then(GitCommitTask.class).message("added some files")
                .then(GitPushTask.class).remote("origin").branch("dummyBranch")
                .then(GitPushTask.class).remote("origin").branch(":dummyBranch") // delete that branch
                .execute().await();
    }
}
