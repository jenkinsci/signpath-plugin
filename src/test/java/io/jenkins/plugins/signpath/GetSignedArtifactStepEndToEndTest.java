package io.jenkins.plugins.signpath;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import hudson.FilePath;
import hudson.model.Result;
import hudson.model.queue.QueueTaskFuture;
import hudson.plugins.git.util.BuildData;
import io.jenkins.plugins.signpath.TestUtils.*;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.InputStream;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import jenkins.model.GlobalConfiguration;
import static org.junit.Assert.*;

@RunWith(Theories.class)
public class GetSignedArtifactStepEndToEndTest {
    private static final int MockServerPort = 51000;

    @Rule
    public final SignPathJenkinsRule j = new SignPathJenkinsRule();

    @Rule
    public final WireMockRule wireMockRule = new WireMockRule(MockServerPort);

    @Theory
    public void getSignedArtifact() throws Exception {
        byte[] signedArtifactBytes = Some.bytes();
        String trustedBuildSystemTokenCredentialId = Some.stringNonEmpty();
        String trustedBuildSystemToken = Some.stringNonEmpty();
        String apiTokenCredentialId = Some.stringNonEmpty();
        String apiToken = Some.stringNonEmpty();
        String organizationId = Some.uuid().toString();
        String signingRequestId = Some.uuid().toString();

        CredentialsStore credentialStore = CredentialStoreUtils.getCredentialStore(j.jenkins);
        assert credentialStore != null;
        CredentialStoreUtils.addCredentials(credentialStore, CredentialsScope.SYSTEM, trustedBuildSystemTokenCredentialId, trustedBuildSystemToken);
        CredentialStoreUtils.addCredentials(credentialStore, CredentialsScope.SYSTEM, apiTokenCredentialId, apiToken);

        String apiUrl = getMockUrl();
        SignPathPluginGlobalConfiguration globalConfig = GlobalConfiguration.all().get(SignPathPluginGlobalConfiguration.class);
        globalConfig.setApiURL(apiUrl);
        globalConfig.setTrustedBuildSystemCredentialId(trustedBuildSystemTokenCredentialId);
        globalConfig.setOrganizationId(organizationId);
        
        wireMockRule.stubFor(get(urlEqualTo("/v1/" + organizationId + "/SigningRequests/" + signingRequestId + "/Status"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{status: 'Completed', workflowStatus: 'Completed', isFinalStatus: true}")));

        wireMockRule.stubFor(get(urlEqualTo("/v1/" + organizationId + "/SigningRequests/" + signingRequestId + "/SignedArtifact"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(signedArtifactBytes)));

        WorkflowJob workflowJob = createWorkflowJob(
            trustedBuildSystemTokenCredentialId,
            apiTokenCredentialId,
            organizationId,
            signingRequestId);

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

        byte[] signedArtifactContent = getSignedArtifactBytes(workflowJob);
        assertArrayEquals(signedArtifactBytes, signedArtifactContent);

        wireMockRule.verify(getRequestedFor(urlEqualTo("/v1/" + organizationId + "/SigningRequests/" + signingRequestId + "/Status"))
                .withHeader("Authorization", equalTo("Bearer " + apiToken)));
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

    private WorkflowJob createWorkflowJob(String trustedBuildSystemTokenCredentialId,
                                          String apiTokenCredentialId,
                                          String organizationId,
                                          String signingRequestId) throws IOException {
        return j.createWorkflow("SignPath",
                "getSignedArtifact(" +
                        "outputArtifactPath: 'signed.exe', " +
                        "trustedBuildSystemTokenCredentialId: '" + trustedBuildSystemTokenCredentialId + "'," +
                        "apiTokenCredentialId: '" + apiTokenCredentialId + "'," +
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

    private byte[] getSignedArtifactBytes(WorkflowJob workflowJob) throws IOException, InterruptedException {
        FilePath workspace = j.jenkins.getWorkspaceFor(workflowJob);
        assert workspace != null;
        try (InputStream in = workspace.child("signed.exe").read()) {
            return in.readAllBytes();
        }
    }
}
