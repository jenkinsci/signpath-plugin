package io.jenkins.plugins.signpath;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import hudson.Launcher;
import hudson.model.FingerprintMap;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.model.queue.QueueTaskFuture;
import hudson.plugins.git.util.BuildData;
import io.jenkins.plugins.signpath.Artifacts.DefaultArtifactFileManager;
import io.jenkins.plugins.signpath.Common.TemporaryFile;
import io.jenkins.plugins.signpath.Exceptions.ArtifactNotFoundException;
import io.jenkins.plugins.signpath.TestUtils.BuildDataDomainObjectMother;
import io.jenkins.plugins.signpath.TestUtils.CredentialStoreUtils;
import io.jenkins.plugins.signpath.TestUtils.Some;
import io.jenkins.plugins.signpath.TestUtils.TemporaryFileUtil;
import io.jenkins.plugins.signpath.TestUtils.WorkflowJobUtil;
import jenkins.model.GlobalConfiguration;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.io.IOException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@WithJenkins
@ExtendWith(WireMockExtension.class)
class GetSignedArtifactStepEndToEndTest {
    private static final int MockServerPort = 51000;

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().port(MockServerPort))
            .build();

    @Test
    void getSignedArtifact(JenkinsRule j) throws Exception {
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

        wireMock.stubFor(get(urlEqualTo("/v1/" + organizationId + "/SigningRequests/" + signingRequestId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{status: 'Completed', workflowStatus: 'Completed', isFinalStatus: true}")));

        wireMock.stubFor(get(urlEqualTo("/v1/" + organizationId + "/SigningRequests/" + signingRequestId + "/SignedArtifact"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(signedArtifactBytes)));

        WorkflowJob workflowJob = WorkflowJobUtil.createWorkflowJob(
            j,
            apiUrl,
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

        byte[] signedArtifactContent = getSignedArtifactBytes(j, run);
        assertArrayEquals(signedArtifactBytes, signedArtifactContent);

        wireMock.verify(getRequestedFor(urlEqualTo("/v1/" + organizationId + "/SigningRequests/" + signingRequestId))
                .withHeader("Authorization", equalTo("Bearer " + apiToken + ":" + trustedBuildSystemToken)));
    }

    @Test
    void getSignedArtifact_withMissingField_fails(JenkinsRule j) throws Exception {
        WorkflowJob workflowJob = WorkflowJobUtil.createWorkflow(j, "SignPath", "getSignedArtifact();");

        BuildData buildData = new BuildData(Some.stringNonEmpty());
        buildData.saveBuild(BuildDataDomainObjectMother.createRandomBuild(1));
        buildData.addRemoteUrl(Some.url());

        // ACT
        QueueTaskFuture<WorkflowRun> runFuture = workflowJob.scheduleBuild2(0, buildData);
        assertNotNull(runFuture);
        WorkflowRun run = runFuture.get();

        // ASSERT
        assertEquals(Result.FAILURE, run.getResult());
        assertTrue(run.getLog().contains("SignPathStepInvalidArgumentException"));
    }

    private static String getMockUrl() {
        return getMockUrl("");
    }

    private static String getMockUrl(String postfix) {
        return String.format("http://localhost:%d/%s", MockServerPort, postfix);
    }

    private static byte[] getSignedArtifactBytes(JenkinsRule j, WorkflowRun run) throws IOException, ArtifactNotFoundException {
        Launcher launcher = j.createLocalLauncher();
        TaskListener listener = j.createTaskListener();
        FingerprintMap fingerprintMap = j.jenkins.getFingerprintMap();
        DefaultArtifactFileManager artifactFileManager = new DefaultArtifactFileManager(fingerprintMap, run, launcher, listener);
        TemporaryFile signedArtifact = artifactFileManager.retrieveArtifact("signed.exe");
        return TemporaryFileUtil.getContentAndDispose(signedArtifact);
    }
}