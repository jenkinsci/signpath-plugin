package io.jenkins.plugins.SignPath.Artifacts;

import hudson.Launcher;
import hudson.model.TaskListener;
import io.jenkins.plugins.SignPath.Common.TemporaryFile;
import io.jenkins.plugins.SignPath.Exceptions.ArtifactNotFoundException;
import io.jenkins.plugins.SignPath.TestUtils.SignPathJenkinsRule;
import io.jenkins.plugins.SignPath.TestUtils.Some;
import io.jenkins.plugins.SignPath.TestUtils.TemporaryFileUtil;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.FromDataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.function.ThrowingRunnable;
import org.junit.runner.RunWith;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

@RunWith(Theories.class)
public class ArtifactFileManagerTest {
    @Rule
    public SignPathJenkinsRule j= new SignPathJenkinsRule();

    @Theory
    public void retrieveArtifact() throws Exception {
        ArtifactFileManager sut = runJob(archiveArtifactScript("hello.txt"));

        // ACT
        TemporaryFile retrievedArtifact = sut.retrieveArtifact("hello.txt");

        // ASSERT
        assertTrue(retrievedArtifact.getAbsolutePath().endsWith("hello.txt"));
        byte[] retrievedArtifactContent = TemporaryFileUtil.getContentAndDispose(retrievedArtifact);
        String retrievedArtifactString = new String(retrievedArtifactContent, StandardCharsets.UTF_8);
        assertEquals("hello", retrievedArtifactString);
    }

    @DataPoints("allFileNames")
    public static String[][] allFileNames() {
        return new String[][]{
                new String[]{"some.exe", "some.exe"},
                new String[]{"subfolder/my.dll", "my.dll"}
        };
    }


    @Theory
    public void retrieveArtifact_ReturnsCorrectFileName(@FromDataPoints("allFileNames") String[] fileNames) throws Exception {
        String artifactPath = fileNames[0];
        String expectedFileName = fileNames[1];

        ArtifactFileManager sut = runJob(archiveArtifactScript(artifactPath));

        // ACT
        TemporaryFile retrievedArtifact = sut.retrieveArtifact(artifactPath);

        // ASSERT
        String message = String.format("%s must end with %s", retrievedArtifact.getAbsolutePath(), expectedFileName);
        assertTrue(message, retrievedArtifact.getAbsolutePath().endsWith(expectedFileName));
    }

    @Theory
    public void retrieveArtifact_WithoutArtifacts_Throws() throws Exception{
        ArtifactFileManager sut = runJob("");

        // ACT
        ThrowingRunnable act = () -> sut.retrieveArtifact("hello.txt");

        // ASSERT
        Throwable ex = assertThrows(ArtifactNotFoundException.class, act);
        assertEquals("The artifact at path \"hello.txt\" was not found.", ex.getMessage() );
    }

    @Theory
    public void retrieveArtifact_WithWrongArtifact_Throws() throws Exception{
        ArtifactFileManager sut = runJob(archiveArtifactScript("hello.txt"));

        // ACT
        ThrowingRunnable act = () -> sut.retrieveArtifact("does not exist.txt");

        // ASSERT
        Throwable ex = assertThrows(ArtifactNotFoundException.class, act);
        assertEquals("The artifact at path \"does not exist.txt\" was not found.", ex.getMessage() );
    }

    @Theory
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
