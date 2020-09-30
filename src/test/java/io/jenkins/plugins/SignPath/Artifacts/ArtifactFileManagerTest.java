package io.jenkins.plugins.SignPath.Artifacts;

import hudson.Launcher;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.model.queue.QueueTaskFuture;
import io.jenkins.plugins.SignPath.Common.TemporaryFile;
import io.jenkins.plugins.SignPath.TestUtils.Some;
import io.jenkins.plugins.SignPath.TestUtils.TemporaryFileUtil;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class ArtifactFileManagerTest {
    @Rule
    public JenkinsRule j= new JenkinsRule();

    private ArtifactFileManager sut;
    private WorkflowRun run;

    @Before
    public void setup() throws IOException, ExecutionException, InterruptedException {
        Launcher launcher = j.createLocalLauncher();
        TaskListener listener = j.createTaskListener();
        WorkflowJob workflowJob = j.createProject(WorkflowJob.class);
        workflowJob.setDefinition(new CpsFlowDefinition(
                "pipeline { stages { stage('test') { steps {"+
                "writeFile text: 'hello', file: 'hello.txt'; " +
                "step([$class: 'ArtifactArchiver', artifacts: 'hello.txt', fingerprint: true]) " +
                        "} } } }",
                true));

        QueueTaskFuture<WorkflowRun> futureRun  = workflowJob.scheduleBuild2(0);
        run = futureRun.get();
        sut = new ArtifactFileManager(run, launcher, listener);
    }

    @Test
    public void retrieveArtifact() throws IOException, InterruptedException {
        Result result= run.getResult();

        // ACT
        TemporaryFile retrievedArtifact = sut.retrieveArtifact("greeting.txt");
        byte[] retrievedArtifactContent = TemporaryFileUtil.getContent(retrievedArtifact);
        String retrievedArtifactString = new String(retrievedArtifactContent, StandardCharsets.UTF_8);

        // ASSERT
        assertEquals("hello", retrievedArtifactString);
    }

    @Test
    public void storeAndRetrieveArtifact() throws IOException, InterruptedException {
        byte[] artifactContent = Some.bytes();
        TemporaryFile artifact = TemporaryFileUtil.create(artifactContent);
        sut.storeArtifact(artifact, "artifact");

        // ACT
        TemporaryFile retrievedArtifact = sut.retrieveArtifact("artifact");
        byte[] retrievedArtifactContent = TemporaryFileUtil.getContent(retrievedArtifact);

        // ASSERT
        assertArrayEquals(artifactContent, retrievedArtifactContent);
        artifact.close();
    }
}
