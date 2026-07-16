package io.jenkins.plugins.signpath;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import hudson.FilePath;
import hudson.model.Result;
import hudson.model.queue.QueueTaskFuture;
import io.jenkins.plugins.signpath.TestUtils.*;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
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
    private static final String SIGNED_ARTIFACT_PATH = "signed.exe";
    private static final String ENDPOINT_SLUG = "JenkinsOnPrem";

    @Rule
    public final SignPathJenkinsRule j = new SignPathJenkinsRule();

    @Rule
    public final WireMockRule wireMockRule = new WireMockRule(MockServerPort);

    @Theory
    public void getSignedArtifact() throws Exception {
        byte[] signedArtifactBytes = Some.bytes();
        String apiTokenCredentialId = Some.stringNonEmpty();
        String apiToken = Some.stringNonEmpty();
        String organizationId = Some.uuid().toString();
        String signingRequestId = Some.uuid().toString();

        CredentialsStore credentialStore = CredentialStoreUtils.getCredentialStore(j.jenkins);
        assert credentialStore != null;
        CredentialStoreUtils.addCredentials(credentialStore, CredentialsScope.SYSTEM, apiTokenCredentialId, apiToken);

        SignPathPluginGlobalConfiguration globalConfig = GlobalConfiguration.all().get(SignPathPluginGlobalConfiguration.class);
        globalConfig.setConnectorURL(getMockUrl());
        globalConfig.setConnectorEndpointSlug(ENDPOINT_SLUG);
        globalConfig.setOrganizationId(organizationId);

        String statusRoute = "/Jenkins/" + ENDPOINT_SLUG + "/" + organizationId + "/SigningRequests/" + signingRequestId + "/Status";
        String signedArtifactRoute = "/Jenkins/" + ENDPOINT_SLUG + "/" + organizationId + "/SigningRequests/" + signingRequestId + "/SignedArtifact";

        wireMockRule.stubFor(get(urlEqualTo(statusRoute))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{status: 'Completed', isFinalStatus: true, webLink: 'https://app.signpath.io/sr'}")));

        wireMockRule.stubFor(get(urlEqualTo(signedArtifactRoute))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(signedArtifactBytes)));

        WorkflowJob workflowJob = createWorkflowJob(
            apiTokenCredentialId,
            organizationId,
            signingRequestId);

        // ACT
        QueueTaskFuture<WorkflowRun> runFuture = workflowJob.scheduleBuild2(0);
        assert runFuture != null;
        WorkflowRun run = runFuture.get();

        // ASSERT
        if (run.getResult() != Result.SUCCESS) {
            assertEquals("", run.getLog() + run.getResult());
            fail();
        }

        byte[] signedArtifactContent = getSignedArtifactBytes(workflowJob);
        assertArrayEquals(signedArtifactBytes, signedArtifactContent);

        wireMockRule.verify(getRequestedFor(urlEqualTo(statusRoute))
                .withHeader("Authorization", equalTo("Bearer " + apiToken)));
        wireMockRule.verify(getRequestedFor(urlEqualTo(signedArtifactRoute))
                .withHeader("Authorization", equalTo("Bearer " + apiToken)));
    }

    @Theory
    public void getSignedArtifact_withMissingField_fails() throws Exception {
        WorkflowJob workflowJob = j.createWorkflow("SignPath", "getSignedArtifact();");

        // ACT
        QueueTaskFuture<WorkflowRun> runFuture = workflowJob.scheduleBuild2(0);
        assert runFuture != null;
        WorkflowRun run = runFuture.get();

        // ASSERT
        assertEquals(Result.FAILURE, run.getResult());
        assertTrue(run.getLog().contains("SignPathStepInvalidArgumentException"));
    }

    private WorkflowJob createWorkflowJob(String apiTokenCredentialId,
                                          String organizationId,
                                          String signingRequestId) throws IOException {
        return j.createWorkflow("SignPath",
                "getSignedArtifact(" +
                        "outputArtifactPath: '" + SIGNED_ARTIFACT_PATH + "', " +
                        "apiTokenCredentialId: '" + apiTokenCredentialId + "'," +
                        "organizationId: '" + organizationId + "'," +
                        "signingRequestId: '" + signingRequestId + "'," +
                        "serviceUnavailableTimeoutInSeconds: 10," +
                        "uploadAndDownloadRequestTimeoutInSeconds: 10," +
                        "waitForCompletionTimeoutInSeconds: 10);");
    }

    private String getMockUrl() {
        return String.format("http://localhost:%d/", MockServerPort);
    }

    private byte[] getSignedArtifactBytes(WorkflowJob workflowJob) throws IOException, InterruptedException {
        FilePath workspace = j.jenkins.getWorkspaceFor(workflowJob);
        assert workspace != null;
        try (InputStream in = workspace.child(SIGNED_ARTIFACT_PATH).read()) {
            return in.readAllBytes();
        }
    }
}
