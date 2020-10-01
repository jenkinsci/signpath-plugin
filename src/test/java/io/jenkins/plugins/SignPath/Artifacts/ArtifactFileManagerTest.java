package io.jenkins.plugins.SignPath.Artifacts;

import hudson.Launcher;
import hudson.model.TaskListener;
import io.jenkins.plugins.SignPath.Common.TemporaryFile;
import io.jenkins.plugins.SignPath.TestUtils.SignPathJenkinsRule;
import io.jenkins.plugins.SignPath.TestUtils.Some;
import io.jenkins.plugins.SignPath.TestUtils.TemporaryFileUtil;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class ArtifactFileManagerTest {
    @Rule
    public SignPathJenkinsRule j= new SignPathJenkinsRule();

    private ArtifactFileManager sut;

    @Before
    public void setup() throws Exception {
        Launcher launcher = j.createLocalLauncher();
        TaskListener listener = j.createTaskListener();

        WorkflowJob workflowJob = j.createWorkflow("SignPath",
                "writeFile text: 'hello', file: 'hello.txt'; " +
                "archiveArtifacts artifacts: 'hello.txt', fingerprint: true ");

        WorkflowRun run = j.assertBuildStatusSuccess(workflowJob.scheduleBuild2(0));
        sut = new ArtifactFileManager(run, launcher, listener);
    }

    @Test
    public void retrieveArtifact() throws IOException {
        // ACT
        TemporaryFile retrievedArtifact = sut.retrieveArtifact("hello.txt");

        // ASSERT
        byte[] retrievedArtifactContent = TemporaryFileUtil.getContentAndDispose(retrievedArtifact);
        String retrievedArtifactString = new String(retrievedArtifactContent, StandardCharsets.UTF_8);
        assertEquals("hello", retrievedArtifactString);
    }

    @Test
    public void storeAndRetrieveArtifact() throws IOException, InterruptedException {
        byte[] artifactContent = Some.bytes();
        TemporaryFile artifact = TemporaryFileUtil.create(artifactContent);
        sut.storeArtifact(artifact, "artifact");

        // ACT
        TemporaryFile retrievedArtifact = sut.retrieveArtifact("artifact");
        byte[] retrievedArtifactContent = TemporaryFileUtil.getContentAndDispose(retrievedArtifact);

        // ASSERT
        assertArrayEquals(artifactContent, retrievedArtifactContent);
        artifact.close();
    }
}
