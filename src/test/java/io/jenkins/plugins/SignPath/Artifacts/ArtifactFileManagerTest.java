package io.jenkins.plugins.SignPath.Artifacts;

import hudson.Launcher;
import hudson.model.TaskListener;
import io.jenkins.plugins.SignPath.Common.TemporaryFile;
import io.jenkins.plugins.SignPath.Exceptions.ArtifactNotFoundException;
import io.jenkins.plugins.SignPath.Exceptions.SignPathStepFailedException;
import io.jenkins.plugins.SignPath.TestUtils.SignPathJenkinsRule;
import io.jenkins.plugins.SignPath.TestUtils.Some;
import io.jenkins.plugins.SignPath.TestUtils.TemporaryFileUtil;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class ArtifactFileManagerTest {
    @Rule
    public SignPathJenkinsRule j= new SignPathJenkinsRule();

    @Test
    public void retrieveArtifact() throws Exception {
        ArtifactFileManager sut = runJob(archiveArtifactScript("hello.txt"));

        // ACT
        TemporaryFile retrievedArtifact = sut.retrieveArtifact("hello.txt");

        // ASSERT
        byte[] retrievedArtifactContent = TemporaryFileUtil.getContentAndDispose(retrievedArtifact);
        String retrievedArtifactString = new String(retrievedArtifactContent, StandardCharsets.UTF_8);
        assertEquals("hello", retrievedArtifactString);
    }


    @Test
    public void retrieveArtifact_WithoutArtifacts_Throws() throws Exception{
        ArtifactFileManager sut = runJob("");

        // ACT
        ThrowingRunnable act = () -> sut.retrieveArtifact("hello.txt");

        // ASSERT
        Throwable ex = assertThrows(ArtifactNotFoundException.class, act);
        assertEquals("The artifact at path \"hello.txt\" was not found.", ex.getMessage() );
    }

    @Test
    public void retrieveArtifact_WithWrongArtifact_Throws() throws Exception{
        ArtifactFileManager sut = runJob(archiveArtifactScript("hello.txt"));

        // ACT
        ThrowingRunnable act = () -> sut.retrieveArtifact("does not exist.txt");

        // ASSERT
        Throwable ex = assertThrows(ArtifactNotFoundException.class, act);
        assertEquals("The artifact at path \"does not exist.txt\" was not found.", ex.getMessage() );
    }

    @Test
    public void storeAndRetrieveArtifact() throws Exception {
        ArtifactFileManager sut = runJob("");

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

    private String archiveArtifactScript(String artifactName) {
        return "writeFile text: 'hello', file: '" + artifactName + "'; " +
                "archiveArtifacts artifacts: '" + artifactName + "', fingerprint: true ";
    }

    private ArtifactFileManager runJob(String script) throws Exception {
        Launcher launcher = j.createLocalLauncher();
        TaskListener listener = j.createTaskListener();
        WorkflowJob workflowJob = j.createWorkflow("SignPath", script);
        WorkflowRun run = j.assertBuildStatusSuccess(workflowJob.scheduleBuild2(0));
        return new ArtifactFileManager(run, launcher, listener);
    }
}
