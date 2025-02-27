package io.jenkins.plugins.signpath;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.github.tomakehurst.wiremock.http.Request;
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
import jenkins.model.JenkinsLocationConfiguration;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.io.IOException;

import static com.github.tomakehurst.wiremock.client.WireMock.aMultipart;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

// PLANNED SIGN-3573: Think of strategy to inject PowerShell Module into the Jenkins PowerShell Session
@WithJenkins
@ExtendWith(WireMockExtension.class)
class SubmitSigningRequestStepEndToEndTest {
    private static final int MockServerPort = 51000;

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().port(MockServerPort))
            .build();

    private JenkinsRule j;

    @BeforeEach
    void setUp(JenkinsRule j) {
        this.j = j;
    }

    @ParameterizedTest(name = "withOptionalFields: {0}")
    @ValueSource(booleans = {true, false})
    void submitSigningRequest(boolean withOptionalFields) throws Exception {
        byte[] signedArtifactBytes = Some.bytes();
        String trustedBuildSystemTokenCredentialId = Some.stringNonEmpty();
        String trustedBuildSystemToken = Some.stringNonEmpty();
        String unsignedArtifactString = Some.stringNonEmpty();
        String apiTokenCredentialId = Some.stringNonEmpty();
        String apiToken = Some.stringNonEmpty();
        String projectSlug = Some.stringNonEmpty();
        String signingPolicySlug = Some.stringNonEmpty();
        String organizationId = Some.uuid().toString();
        String artifactConfigurationSlug = Some.stringNonEmpty();
        String description = Some.stringNonEmpty();
        String signingRequestId = Some.uuid().toString();
        String userDefinedParamName = "UserDefinedParam";
        String userDefinedParamValue = Some.stringNonEmpty();

        CredentialsStore credentialStore = CredentialStoreUtils.getCredentialStore(j.jenkins);
        assert credentialStore != null;
        CredentialStoreUtils.addCredentials(credentialStore, CredentialsScope.SYSTEM, trustedBuildSystemTokenCredentialId, trustedBuildSystemToken);
        CredentialStoreUtils.addCredentials(credentialStore, CredentialsScope.SYSTEM, apiTokenCredentialId, apiToken);

        String apiUrl = getMockUrl();
        String downloadSignedArtifact = "downloadSignedArtifact";

        wireMock.stubFor(post(urlEqualTo("/v1/" + organizationId + "/SigningRequests"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{signingRequestId: '" + signingRequestId + "'}")
                        .withHeader("Location", getMockUrl("v1/" + organizationId + "/SigningRequests/" + signingRequestId))));

        wireMock.stubFor(get(urlEqualTo("/v1/" + organizationId + "/SigningRequests/" + signingRequestId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{status: 'Completed', workflowStatus: 'Completed', isFinalStatus: true, signedArtifactLink: '" + getMockUrl(downloadSignedArtifact) + "'}")));

        wireMock.stubFor(get(urlEqualTo("/" + downloadSignedArtifact))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(signedArtifactBytes)));

        wireMock.stubFor(get(urlEqualTo("/v1/" + organizationId + "/SigningRequests/" + signingRequestId + "/SignedArtifact"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(signedArtifactBytes)));

        SignPathPluginGlobalConfiguration globalConfig = GlobalConfiguration.all().get(SignPathPluginGlobalConfiguration.class);
        globalConfig.setApiURL(apiUrl);
        globalConfig.setTrustedBuildSystemCredentialId(trustedBuildSystemTokenCredentialId);
        WorkflowJob workflowJob = withOptionalFields
                ? WorkflowJobUtil.createWorkflowJobWithOptionalParameters(j, apiUrl, trustedBuildSystemTokenCredentialId, apiTokenCredentialId, organizationId, projectSlug, signingPolicySlug, unsignedArtifactString, artifactConfigurationSlug, description, userDefinedParamName, userDefinedParamValue, true)
                : WorkflowJobUtil.createWorkflowJob(j, apiUrl, trustedBuildSystemTokenCredentialId, apiTokenCredentialId, organizationId, projectSlug, signingPolicySlug, unsignedArtifactString, true);

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

        assertTrue(run.getLog().contains("<returnValue>:\"" + signingRequestId + "\""));

        if (withOptionalFields) {
            assertRequest(apiToken, trustedBuildSystemToken, unsignedArtifactString, remoteUrl, organizationId, projectSlug, signingPolicySlug, artifactConfigurationSlug, userDefinedParamName, userDefinedParamValue, description);
        } else {
            assertRequest(apiToken, trustedBuildSystemToken, unsignedArtifactString, remoteUrl, organizationId, projectSlug, signingPolicySlug);
        }
    }

    @ParameterizedTest(name = "withOptionalFields: {0}")
    @ValueSource(booleans = {true, false})
    void submitSigningRequest_withoutWaitForCompletion(boolean withOptionalFields) throws Exception {
        String unsignedArtifactString = Some.stringNonEmpty();
        String trustedBuildSystemTokenCredentialId = Some.stringNonEmpty();
        String trustedBuildSystemToken = Some.stringNonEmpty();
        String apiTokenCredentialId = Some.stringNonEmpty();
        String apiToken = Some.stringNonEmpty();
        String projectSlug = Some.stringNonEmpty();
        String signingPolicySlug = Some.stringNonEmpty();
        String organizationId = Some.uuid().toString();
        String artifactConfigurationSlug = Some.stringNonEmpty();
        String description = Some.stringNonEmpty();
        String signingRequestId = Some.uuid().toString();
        String userDefinedParamName = "UserDefinedParam";
        String userDefinedParamValue = Some.stringNonEmpty();

        CredentialsStore credentialStore = CredentialStoreUtils.getCredentialStore(j.jenkins);
        assert credentialStore != null;
        CredentialStoreUtils.addCredentials(credentialStore, CredentialsScope.SYSTEM, trustedBuildSystemTokenCredentialId, trustedBuildSystemToken);
        CredentialStoreUtils.addCredentials(credentialStore, CredentialsScope.SYSTEM, apiTokenCredentialId, apiToken);

        String apiUrl = getMockUrl();
        wireMock.stubFor(post(urlEqualTo("/v1/" + organizationId + "/SigningRequests"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{signingRequestId: '" + signingRequestId + "'}")
                        .withHeader("Location", getMockUrl("v1/" + organizationId + "/SigningRequests/" + signingRequestId))));

        SignPathPluginGlobalConfiguration globalConfig = GlobalConfiguration.all().get(SignPathPluginGlobalConfiguration.class);
        globalConfig.setApiURL(apiUrl);
        globalConfig.setTrustedBuildSystemCredentialId(trustedBuildSystemTokenCredentialId);

        WorkflowJob workflowJob = withOptionalFields
                ? WorkflowJobUtil.createWorkflowJobWithOptionalParameters(j, apiUrl, trustedBuildSystemTokenCredentialId, apiTokenCredentialId, organizationId, projectSlug, signingPolicySlug, unsignedArtifactString, artifactConfigurationSlug, description, userDefinedParamName, userDefinedParamValue, false)
                : WorkflowJobUtil.createWorkflowJob(j, apiUrl, trustedBuildSystemTokenCredentialId, apiTokenCredentialId, organizationId, projectSlug, signingPolicySlug, unsignedArtifactString, false);

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

        assertTrue(run.getLog().contains("<returnValue>:\"" + signingRequestId + "\""));

        if (withOptionalFields) {
            assertRequest(apiToken, trustedBuildSystemToken, unsignedArtifactString, remoteUrl, organizationId, projectSlug, signingPolicySlug, artifactConfigurationSlug, userDefinedParamName, userDefinedParamValue, description);
        } else {
            assertRequest(apiToken, trustedBuildSystemToken, unsignedArtifactString, remoteUrl, organizationId, projectSlug, signingPolicySlug);
        }
    }

    @Test
    void submitSigningRequest_withMissingField_fails() throws Exception {
        WorkflowJob workflowJob = WorkflowJobUtil.createWorkflow(j, "SignPath", "submitSigningRequest();");

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

    @ParameterizedTest
    @ValueSource(strings = {"", "not a valid url"})
    void submitSigningRequest_withWrongOrMissingRootUrl_fails(String rootUrl) throws Exception {
        String organizationId = Some.uuid().toString();

        SignPathPluginGlobalConfiguration globalConfig = GlobalConfiguration.all().get(SignPathPluginGlobalConfiguration.class);
        String mockUrl = getMockUrl();
        String tbsToken = Some.stringNonEmpty();
        globalConfig.setApiURL(mockUrl);
        globalConfig.setTrustedBuildSystemCredentialId(tbsToken);

        WorkflowJob workflowJob = WorkflowJobUtil.createWorkflowJob(j, mockUrl, tbsToken, Some.stringNonEmpty(), organizationId, Some.stringNonEmpty(), Some.stringNonEmpty(), Some.stringNonEmpty(), false);

        BuildData buildData = new BuildData(Some.stringNonEmpty());
        buildData.saveBuild(BuildDataDomainObjectMother.createRandomBuild(1));
        buildData.addRemoteUrl(Some.url());

        // we set a missing root-url
        JenkinsLocationConfiguration.get().setUrl(rootUrl);

        // ACT
        QueueTaskFuture<WorkflowRun> runFuture = workflowJob.scheduleBuild2(0, buildData);
        assertNotNull(runFuture);
        WorkflowRun run = runFuture.get();

        // ASSERT
        assertEquals(Result.FAILURE, run.getResult());
        assertTrue(run.getLog().contains("SignPathStepInvalidArgumentException"), run.getLog());
        wireMock.verify(exactly(0), postRequestedFor(urlEqualTo("/v1/" + organizationId + "/SigningRequests")));
    }

    private static void assertRequest(
            String apiToken,
            String trustedBuildSystemToken,
            String unsignedArtifactString,
            String remoteUrl,
            String organizationId,
            String projectSlug,
            String signingPolicySlug,
            String artifactConfigurationSlug,
            String userDefinedParamName,
            String userDefinedParamValue,
            String description) {

        wireMock.verify(postRequestedFor(urlEqualTo("/v1/" + organizationId + "/SigningRequests"))
                .withHeader("Authorization", equalTo("Bearer " + apiToken + ":" + trustedBuildSystemToken))
                .withRequestBodyPart(aMultipart().withBody(equalTo(projectSlug)).build())
                .withRequestBodyPart(aMultipart().withBody(equalTo(signingPolicySlug)).build())
                .withRequestBodyPart(aMultipart().withBody(equalTo(remoteUrl)).build())
                .withRequestBodyPart(aMultipart().withBody(equalTo(description)).build())
                .withRequestBodyPart(aMultipart().withBody(equalTo(artifactConfigurationSlug)).build())
                .withRequestBodyPart(aMultipart().withBody(equalTo(userDefinedParamValue)).build())
                .withRequestBodyPart(aMultipart().withName("Parameters." + userDefinedParamName).build()));

        assertFormFiles(unsignedArtifactString, organizationId);
    }

    private static void assertFormFiles(String unsignedArtifactString, String organizationId) {
        Request r = wireMock.findAll(postRequestedFor(urlEqualTo("/v1/" + organizationId + "/SigningRequests"))).get(0);
        assertTrue(getMultipartFormDataFileContents(r, "Artifact").contains(unsignedArtifactString));
        String buildSettingsFile = getMultipartFormDataFileContents(r, "Origin.BuildData.BuildSettingsFile");
        assertTrue(buildSettingsFile.contains("node {writeFile text:"));
        assertTrue(buildSettingsFile.contains(organizationId));
    }

    private static void assertRequest(
            String apiToken,
            String trustedBuildSystemToken,
            String unsignedArtifactString,
            String remoteUrl,
            String organizationId,
            String projectSlug,
            String signingPolicySlug) {

        wireMock.verify(postRequestedFor(urlEqualTo("/v1/" + organizationId + "/SigningRequests"))
                .withHeader("Authorization", equalTo("Bearer " + apiToken + ":" + trustedBuildSystemToken))
                .withRequestBodyPart(aMultipart().withBody(equalTo(projectSlug)).build())
                .withRequestBodyPart(aMultipart().withBody(equalTo(signingPolicySlug)).build())
                .withRequestBodyPart(aMultipart().withBody(equalTo(remoteUrl)).build()));

        assertFormFiles(unsignedArtifactString, organizationId);
    }


    private static String getMultipartFormDataFileContents(Request r, String name) {

        for (Request.Part part : r.getParts()) {
            if(name.equals(part.getName())) {
                return part.getBody().asString();
            }
        }
        return fail("multipart-form-data with name " + name + " not found in " + r.getBodyAsString());
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