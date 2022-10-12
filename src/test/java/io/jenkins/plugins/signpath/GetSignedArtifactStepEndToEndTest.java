package io.jenkins.plugins.signpath;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import hudson.Launcher;
import hudson.model.FingerprintMap;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.model.queue.QueueTaskFuture;
import hudson.plugins.git.util.BuildData;
import io.jenkins.plugins.signpath.Artifacts.DefaultArtifactFileManager;
import io.jenkins.plugins.signpath.Common.TemporaryFile;
import io.jenkins.plugins.signpath.Exceptions.ArtifactNotFoundException;
import io.jenkins.plugins.signpath.TestUtils.*;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import java.io.IOException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.*;

@RunWith(Theories.class)
public class GetSignedArtifactStepEndToEndTest {
    private static final int MockServerPort = 51000;

    @ClassRule
    public static final PortablePowerShellRule ps = new PortablePowerShellRule(true);

    @Rule
    public final SignPathJenkinsRule j = new SignPathJenkinsRule(ps.getPowerShellExecutable());

    @Rule
    public final WireMockRule wireMockRule = new WireMockRule(MockServerPort);

    @Theory
    public void getSignedArtifact() throws Exception {
        byte[] signedArtifactBytes = Some.bytes();
        String trustedBuildSystemTokenCredentialId = Some.stringNonEmpty();
        String trustedBuildSystemToken = Some.stringNonEmpty();
        String ciUserTokenCredentialId = Some.stringNonEmpty();
        String ciUserToken = Some.stringNonEmpty();
        String organizationId = Some.uuid().toString();
        String signingRequestId = Some.uuid().toString();

        CredentialsStore credentialStore = CredentialStoreUtils.getCredentialStore(j.jenkins);
        assert credentialStore != null;
        CredentialStoreUtils.addCredentials(credentialStore, CredentialsScope.SYSTEM, trustedBuildSystemTokenCredentialId, trustedBuildSystemToken);
        CredentialStoreUtils.addCredentials(credentialStore, CredentialsScope.SYSTEM, ciUserTokenCredentialId, ciUserToken);

        String apiUrl = getMockUrl();
        
        wireMockRule.stubFor(get(urlEqualTo("/v1/" + organizationId + "/SigningRequests/" + signingRequestId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{status: 'Completed', workflowStatus: 'Completed', isFinalStatus: true}")));

        wireMockRule.stubFor(get(urlEqualTo("/v1/" + organizationId + "/SigningRequests/" + signingRequestId + "/SignedArtifact"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(signedArtifactBytes)));

        WorkflowJob workflowJob = createWorkflowJob(apiUrl, trustedBuildSystemTokenCredentialId, ciUserTokenCredentialId, organizationId, signingRequestId);

        String remoteUrl = Some.url();
        BuildData buildData = new BuildData(Some.stringNonEmpty());
        buildData.saveBuild(BuildDataDomainObjectMother.createRandomBuild(1));
        buildData.addRemoteUrl(remoteUrl);

        // ACT
        QueueTaskFuture<WorkflowRun> runFuture = workflowJob.scheduleBuild2(0, buildData);
        assert runFuture != null;
        WorkflowRun run = runFuture.get();

        // ASSERT
        if (run.getResult() != Result.SUCCESS) {
            assertEquals("", run.getLog() + run.getResult());
            fail();
        }

        byte[] signedArtifactContent = getSignedArtifactBytes(run);
        assertArrayEquals(signedArtifactBytes, signedArtifactContent);

        wireMockRule.verify(getRequestedFor(urlEqualTo("/v1/" + organizationId + "/SigningRequests/" + signingRequestId))
                .withHeader("Authorization", equalTo("Bearer " + ciUserToken + ":" + trustedBuildSystemToken)));
    }

    @Theory
    public void getSignedArtifact_withMissingField_fails() throws Exception {
        WorkflowJob workflowJob = j.createWorkflow("SignPath", "getSignedArtifact();");

        BuildData buildData = new BuildData(Some.stringNonEmpty());
        buildData.saveBuild(BuildDataDomainObjectMother.createRandomBuild(1));
        buildData.addRemoteUrl(Some.url());

        // ACT
        QueueTaskFuture<WorkflowRun> runFuture = workflowJob.scheduleBuild2(0, buildData);
        assert runFuture != null;
        WorkflowRun run = runFuture.get();

        // ASSERT
        assertEquals(Result.FAILURE, run.getResult());
        assertTrue(run.getLog().contains("SignPathStepInvalidArgumentException"));
    }

    private WorkflowJob createWorkflowJob(String apiUrl,
                                          String trustedBuildSystemTokenCredentialId,
                                          String ciUserTokenCredentialId,
                                          String organizationId,
                                          String signingRequestId) throws IOException {
        return j.createWorkflow("SignPath",
                "getSignedArtifact( apiUrl: '" + apiUrl + "', " +
                        "outputArtifactPath: 'signed.exe', " +
                        "trustedBuildSystemTokenCredentialId: '" + trustedBuildSystemTokenCredentialId + "'," +
                        "ciUserTokenCredentialId: '" + ciUserTokenCredentialId + "'," +
                        "organizationId: '" + organizationId + "'," +
                        "signingRequestId: '" + signingRequestId + "'," +
                        "serviceUnavailableTimeoutInSeconds: 10," +
                        "uploadAndDownloadRequestTimeoutInSeconds: 10," +
                        "waitForCompletionTimeoutInSeconds: 10);");
    }

    private String getMockUrl() {
        return getMockUrl("");
    }

    private String getMockUrl(String postfix) {
        return String.format("http://localhost:%d/%s", MockServerPort, postfix);
    }

    private byte[] getSignedArtifactBytes(WorkflowRun run) throws IOException, ArtifactNotFoundException {
        Launcher launcher = j.createLocalLauncher();
        TaskListener listener = j.createTaskListener();
        FingerprintMap fingerprintMap = j.jenkins.getFingerprintMap();
        DefaultArtifactFileManager artifactFileManager = new DefaultArtifactFileManager(fingerprintMap, run, launcher, listener);
        TemporaryFile signedArtifact = artifactFileManager.retrieveArtifact("signed.exe");
        return TemporaryFileUtil.getContentAndDispose(signedArtifact);
    }
}