package io.jenkins.plugins.signpath.Artifacts;

import hudson.Launcher;
import hudson.model.Fingerprint;
import hudson.model.FingerprintMap;
import hudson.model.TaskListener;
import io.jenkins.plugins.signpath.Common.TemporaryFile;
import io.jenkins.plugins.signpath.Exceptions.ArtifactNotFoundException;
import io.jenkins.plugins.signpath.TestUtils.Some;
import io.jenkins.plugins.signpath.TestUtils.TemporaryFileUtil;
import io.jenkins.plugins.signpath.TestUtils.WorkflowJobUtil;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@WithJenkins
class DefaultArtifactFileManagerTest {

    private JenkinsRule j;

    @BeforeEach
    void setUp(JenkinsRule j) {
        this.j = j;
    }

    @Test
    void retrieveArtifact() throws Exception {
        DefaultArtifactFileManager sut = runJob(j, archiveArtifactScript("hello.txt"));

        // ACT
        TemporaryFile retrievedArtifact = sut.retrieveArtifact("hello.txt");

        // ASSERT
        assertTrue(retrievedArtifact.getAbsolutePath().endsWith("hello.txt"));
        byte[] retrievedArtifactContent = TemporaryFileUtil.getContentAndDispose(retrievedArtifact);
        String retrievedArtifactString = new String(retrievedArtifactContent, StandardCharsets.UTF_8);
        assertEquals("hello", retrievedArtifactString);
    }

    @Test
    void retrieveArtifact_fromParent_throws() throws Exception {
        DefaultArtifactFileManager sut = runJob(j, "");

        // ACT
        Executable act = () -> sut.retrieveArtifact("../build.xml");

        // ASSERT
        Throwable ex = assertThrows(IllegalAccessError.class, act);
        assertEquals("artifactPath cannot be in parent directory.", ex.getMessage());
    }


    static String[][] allFileNames() {
        return new String[][]{
                new String[]{"some.exe", "some.exe", "some.exe"},
                new String[]{"subfolder/my.dll", "subfolder/my.dll", "my.dll"},
                new String[]{"subfolder/my.dll", "subfolder/my.dll/", "my.dll"}
        };
    }

    @ParameterizedTest
    @MethodSource("allFileNames")
    void retrieveArtifact_returnsCorrectFileName(String artifactPath, String artifactRetrievalPath, String expectedFileName) throws Exception {
        DefaultArtifactFileManager sut = runJob(j, archiveArtifactScript(artifactPath));

        // ACT
        TemporaryFile retrievedArtifact = sut.retrieveArtifact(artifactRetrievalPath);

        // ASSERT
        String message = String.format("%s must end with %s", retrievedArtifact.getAbsolutePath(), expectedFileName);
        assertTrue(retrievedArtifact.getAbsolutePath().endsWith(expectedFileName), message);
    }

    @Test
    void retrieveArtifact_withoutArtifacts_throws() throws Exception {
        DefaultArtifactFileManager sut = runJob(j, "");

        // ACT
        Executable act = () -> sut.retrieveArtifact("hello.txt");

        // ASSERT
        Throwable ex = assertThrows(ArtifactNotFoundException.class, act);
        assertEquals("The artifact at path 'hello.txt' was not found.", ex.getMessage());
    }

    @Test
    void retrieveArtifact_withWrongArtifact_throws() throws Exception {
        DefaultArtifactFileManager sut = runJob(j, archiveArtifactScript("hello.txt"));

        // ACT
        Executable act = () -> sut.retrieveArtifact("does not exist.txt");

        // ASSERT
        Throwable ex = assertThrows(ArtifactNotFoundException.class, act);
        assertEquals("The artifact at path 'does not exist.txt' was not found.", ex.getMessage());
    }

    @Test
    void storeAndRetrieveArtifact() throws Exception {
        DefaultArtifactFileManager sut = runJob(j, "");

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

    @ParameterizedTest
    @MethodSource("allFileNames")
    void storeArtifact(String artifactPath, String fileNameToStore, String expectedFileName) throws Exception {
        DefaultArtifactFileManager sut = runJob(j, "");

        byte[] artifactContent = Some.bytes();
        TemporaryFile artifact = TemporaryFileUtil.create(artifactContent);

        // ACT
        sut.storeArtifact(artifact, fileNameToStore);

        // ASSERT
        String expectedHash = TemporaryFileUtil.getDigestAndDispose(artifact);
        Fingerprint fingerprint = j.jenkins.getFingerprintMap().get(expectedHash);
        assertNotNull(fingerprint);
        assertEquals(expectedFileName, fingerprint.getFileName());
        assertEquals(expectedHash, fingerprint.getHashString());
    }

    @Test
    void storeArtifact_inParent_throws() throws Exception {
        DefaultArtifactFileManager sut = runJob(j, "");

        byte[] artifactContent = Some.bytes();
        TemporaryFile artifact = TemporaryFileUtil.create(artifactContent);

        // ACT
        Executable act = () -> sut.storeArtifact(artifact, "../Please store me in parent.txt");

        // ASSERT
        Throwable ex = assertThrows(IllegalAccessError.class, act);
        assertEquals("targetArtifactPath cannot be in parent directory.", ex.getMessage());
    }

    private static String archiveArtifactScript(String artifactName) {
        return String.format("writeFile text: 'hello', file: '%s'; archiveArtifacts artifacts: '%s', fingerprint: true ", artifactName, artifactName);
    }

    private static DefaultArtifactFileManager runJob(JenkinsRule j, String script) throws Exception {
        Launcher launcher = j.createLocalLauncher();
        TaskListener listener = j.createTaskListener();
        FingerprintMap fingerprintMap = j.jenkins.getFingerprintMap();

        WorkflowJob workflowJob = WorkflowJobUtil.createWorkflow(j, "SignPath", script);
        WorkflowRun run = j.assertBuildStatusSuccess(workflowJob.scheduleBuild2(0));
        return new DefaultArtifactFileManager(fingerprintMap, run, launcher, listener);
    }
}
