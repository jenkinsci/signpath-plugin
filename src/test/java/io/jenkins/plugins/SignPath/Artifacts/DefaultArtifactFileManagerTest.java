package io.jenkins.plugins.SignPath.Artifacts;

import hudson.Launcher;
import hudson.model.Fingerprint;
import hudson.model.FingerprintMap;
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
public class DefaultArtifactFileManagerTest {
    @Rule
    public SignPathJenkinsRule j = new SignPathJenkinsRule();

    @Theory
    public void retrieveArtifact() throws Exception {
        DefaultArtifactFileManager sut = runJob(archiveArtifactScript("hello.txt"));

        // ACT
        TemporaryFile retrievedArtifact = sut.retrieveArtifact("hello.txt");

        // ASSERT
        assertTrue(retrievedArtifact.getAbsolutePath().endsWith("hello.txt"));
        byte[] retrievedArtifactContent = TemporaryFileUtil.getContentAndDispose(retrievedArtifact);
        String retrievedArtifactString = new String(retrievedArtifactContent, StandardCharsets.UTF_8);
        assertEquals("hello", retrievedArtifactString);
    }

    @Theory
    public void retrieveArtifact_fromParent_throws() throws Exception {
        DefaultArtifactFileManager sut = runJob("");

        // ACT
        ThrowingRunnable act = () -> sut.retrieveArtifact("../build.xml");

        // ASSERT
        Throwable ex = assertThrows(IllegalAccessError.class, act);
        assertEquals("artifactPath cannot be in parent directory.", ex.getMessage());
    }

    @DataPoints("allFileNames")
    public static String[][] allFileNames() {
        return new String[][]{
                new String[]{"some.exe","some.exe", "some.exe"},
                new String[]{"subfolder/my.dll","subfolder/my.dll", "my.dll"},
                new String[]{"subfolder/my.dll","subfolder\\my.dll", "my.dll"},
        };
    }


    @Theory
    public void retrieveArtifact_returnsCorrectFileName(@FromDataPoints("allFileNames") String[] fileNames) throws Exception {
        String artifactPath = fileNames[0];
        String artifactRetrievalPath = fileNames[1];
        String expectedFileName = fileNames[2];

        DefaultArtifactFileManager sut = runJob(archiveArtifactScript(artifactPath));

        // ACT
        TemporaryFile retrievedArtifact = sut.retrieveArtifact(artifactRetrievalPath);

        // ASSERT
        String message = String.format("%s must end with %s", retrievedArtifact.getAbsolutePath(), expectedFileName);
        assertTrue(message, retrievedArtifact.getAbsolutePath().endsWith(expectedFileName));
    }

    @Theory
    public void retrieveArtifact_withoutArtifacts_throws() throws Exception {
        DefaultArtifactFileManager sut = runJob("");

        // ACT
        ThrowingRunnable act = () -> sut.retrieveArtifact("hello.txt");

        // ASSERT
        Throwable ex = assertThrows(ArtifactNotFoundException.class, act);
        assertEquals("The artifact at path \"hello.txt\" was not found.", ex.getMessage());
    }

    @Theory
    public void retrieveArtifact_withWrongArtifact_throws() throws Exception {
        DefaultArtifactFileManager sut = runJob(archiveArtifactScript("hello.txt"));

        // ACT
        ThrowingRunnable act = () -> sut.retrieveArtifact("does not exist.txt");

        // ASSERT
        Throwable ex = assertThrows(ArtifactNotFoundException.class, act);
        assertEquals("The artifact at path \"does not exist.txt\" was not found.", ex.getMessage());
    }

    @Theory
    public void storeAndRetrieveArtifact() throws Exception {
        DefaultArtifactFileManager sut = runJob("");

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

    @Theory
    public void storeArtifact(@FromDataPoints("allFileNames") String[] fileNames) throws Exception {
        String fileNameToStore = fileNames[1];
        String expectedFileName = fileNames[2];

        DefaultArtifactFileManager sut = runJob("");

        byte[] artifactContent = Some.bytes();
        TemporaryFile artifact = TemporaryFileUtil.create(artifactContent);

        // ACT
        sut.storeArtifact(artifact, fileNameToStore);

        // ASSERT
        String expectedHash =  TemporaryFileUtil.getDigestAndDispose(artifact);
        Fingerprint fingerprint = j.jenkins.getFingerprintMap().get(expectedHash);
        assertNotNull(fingerprint);
        assertEquals(expectedFileName,fingerprint.getFileName());
        assertEquals(expectedHash,fingerprint.getHashString());
    }

    @Theory
    public void storeArtifact_inParent_throws() throws Exception {
        DefaultArtifactFileManager sut = runJob("");

        byte[] artifactContent = Some.bytes();
        TemporaryFile artifact = TemporaryFileUtil.create(artifactContent);

        // ACT
        ThrowingRunnable act = () -> sut.storeArtifact(artifact, "../Please store me in parent.txt");

        // ASSERT
        Throwable ex = assertThrows(IllegalAccessError.class, act);
        assertEquals("targetArtifactPath cannot be in parent directory.", ex.getMessage());
    }

    private String archiveArtifactScript(String artifactName) {
        return "writeFile text: 'hello', file: '" + artifactName + "'; " +
                "archiveArtifacts artifacts: '" + artifactName + "', fingerprint: true ";
    }

    private DefaultArtifactFileManager runJob(String script) throws Exception {
        Launcher launcher = j.createLocalLauncher();
        TaskListener listener = j.createTaskListener();
        FingerprintMap fingerprintMap = j.jenkins.getFingerprintMap();
        WorkflowJob workflowJob = j.createWorkflow("SignPath", script);
        WorkflowRun run = j.assertBuildStatusSuccess(workflowJob.scheduleBuild2(0));
        return new DefaultArtifactFileManager(fingerprintMap, run, launcher, listener);
    }
}
