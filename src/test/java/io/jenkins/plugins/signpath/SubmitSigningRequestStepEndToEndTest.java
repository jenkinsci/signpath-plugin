package io.jenkins.plugins.signpath;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.github.tomakehurst.wiremock.http.Request;
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
import jenkins.model.JenkinsLocationConfiguration;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.FromDataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import jenkins.model.GlobalConfiguration;
import static org.junit.Assert.*;

// PLANNED SIGN-3573: Think of strategy to inject PowerShell Module into the Jenkins PowerShell Session
@RunWith(Theories.class)
public class SubmitSigningRequestStepEndToEndTest {
    private static final int MockServerPort = 51000;

    @Rule
    public final SignPathJenkinsRule j = new SignPathJenkinsRule();

    @Rule
    public final WireMockRule wireMockRule = new WireMockRule(MockServerPort);

    @Theory
    public void submitSigningRequest(@FromDataPoints("allBooleans") boolean withOptionalFields) throws Exception {
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
        String uploadPath = "/v1/" + organizationId + "/SigningRequests/" + signingRequestId + "/UploadUnsignedArtifact";

        stubSubmitWithoutArtifact(organizationId, signingRequestId, getMockUrl(uploadPath.substring(1)));
        stubUploadUnsignedArtifact(uploadPath);
        stubGetSigningRequestCompleted(organizationId, signingRequestId, signedArtifactBytes);

        SignPathPluginGlobalConfiguration globalConfig = GlobalConfiguration.all().get(SignPathPluginGlobalConfiguration.class);
        globalConfig.setApiURL(apiUrl);
        globalConfig.setTrustedBuildSystemCredentialId(trustedBuildSystemTokenCredentialId);
        WorkflowJob workflowJob = withOptionalFields
                ? createWorkflowJobWithOptionalParameters(apiUrl, trustedBuildSystemTokenCredentialId, apiTokenCredentialId, organizationId, projectSlug, signingPolicySlug, unsignedArtifactString, artifactConfigurationSlug, description, userDefinedParamName, userDefinedParamValue, true)
                : createWorkflowJob(apiUrl, trustedBuildSystemTokenCredentialId, apiTokenCredentialId, organizationId, projectSlug, signingPolicySlug, unsignedArtifactString, true);

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
        assertTrue(run.getLog().contains("<returnValue>:\"" + signingRequestId + "\""));

        if (withOptionalFields) {
            assertSubmitWithoutArtifactRequest(apiToken, trustedBuildSystemToken, unsignedArtifactString,
                    remoteUrl, organizationId, projectSlug, signingPolicySlug,
                    artifactConfigurationSlug, userDefinedParamName, userDefinedParamValue, description);
        } else {
            assertSubmitWithoutArtifactRequest(apiToken, trustedBuildSystemToken, unsignedArtifactString,
                    remoteUrl, organizationId, projectSlug, signingPolicySlug);
        }
        assertUploadRequest(uploadPath, unsignedArtifactString);
        // The old artifact-with-file route must NOT have been called
        wireMockRule.verify(exactly(0), postRequestedFor(urlEqualTo("/v1/" + organizationId + "/SigningRequests")));
    }

    @Theory
    public void submitSigningRequest_withoutWaitForCompletion(@FromDataPoints("allBooleans") boolean withOptionalFields) throws Exception {
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
        String uploadPath = "/v1/" + organizationId + "/SigningRequests/" + signingRequestId + "/UploadUnsignedArtifact";

        stubSubmitWithoutArtifact(organizationId, signingRequestId, getMockUrl(uploadPath.substring(1)));
        stubUploadUnsignedArtifact(uploadPath);

        SignPathPluginGlobalConfiguration globalConfig = GlobalConfiguration.all().get(SignPathPluginGlobalConfiguration.class);
        globalConfig.setApiURL(apiUrl);
        globalConfig.setTrustedBuildSystemCredentialId(trustedBuildSystemTokenCredentialId);

        WorkflowJob workflowJob = withOptionalFields
                ? createWorkflowJobWithOptionalParameters(apiUrl, trustedBuildSystemTokenCredentialId, apiTokenCredentialId, organizationId, projectSlug, signingPolicySlug, unsignedArtifactString, artifactConfigurationSlug, description, userDefinedParamName, userDefinedParamValue, false)
                : createWorkflowJob(apiUrl, trustedBuildSystemTokenCredentialId, apiTokenCredentialId, organizationId, projectSlug, signingPolicySlug, unsignedArtifactString, false);

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
            assertSubmitWithoutArtifactRequest(apiToken, trustedBuildSystemToken, unsignedArtifactString,
                    remoteUrl, organizationId, projectSlug, signingPolicySlug,
                    artifactConfigurationSlug, userDefinedParamName, userDefinedParamValue, description);
        } else {
            assertSubmitWithoutArtifactRequest(apiToken, trustedBuildSystemToken, unsignedArtifactString,
                    remoteUrl, organizationId, projectSlug, signingPolicySlug);
        }
        assertUploadRequest(uploadPath, unsignedArtifactString);
        wireMockRule.verify(exactly(0), postRequestedFor(urlEqualTo("/v1/" + organizationId + "/SigningRequests")));
    }

    @Theory
    public void submitSigningRequest_onlyHashFileIsUploadedToJenkins() throws Exception {
        String unsignedArtifactString = Some.stringNonEmpty();
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
        String uploadPath = "/v1/" + organizationId + "/SigningRequests/" + signingRequestId + "/UploadUnsignedArtifact";

        stubSubmitWithoutArtifact(organizationId, signingRequestId, getMockUrl(uploadPath.substring(1)));
        stubUploadUnsignedArtifact(uploadPath);

        SignPathPluginGlobalConfiguration globalConfig = GlobalConfiguration.all().get(SignPathPluginGlobalConfiguration.class);
        globalConfig.setApiURL(apiUrl);
        globalConfig.setTrustedBuildSystemCredentialId(trustedBuildSystemTokenCredentialId);

        WorkflowJob workflowJob = createWorkflowJob(apiUrl, trustedBuildSystemTokenCredentialId, apiTokenCredentialId,
                organizationId, Some.stringNonEmpty(), Some.stringNonEmpty(), unsignedArtifactString, false);

        BuildData buildData = new BuildData(Some.stringNonEmpty());
        buildData.saveBuild(BuildDataDomainObjectMother.createRandomBuild(1));
        buildData.addRemoteUrl(Some.url());

        // ACT
        QueueTaskFuture<WorkflowRun> runFuture = workflowJob.scheduleBuild2(0, buildData);
        assert runFuture != null;
        WorkflowRun run = runFuture.get();

        // ASSERT
        if (run.getResult() != Result.SUCCESS) {
            assertEquals("", run.getLog() + run.getResult());
            fail();
        }

        Launcher launcher = j.createLocalLauncher();
        TaskListener listener = j.createTaskListener();
        FingerprintMap fingerprintMap = j.jenkins.getFingerprintMap();
        DefaultArtifactFileManager artifactFileManager = new DefaultArtifactFileManager(fingerprintMap, run, launcher, listener);

        // REVIEW: simplify and unify hashing functionality (see other comments about hashing)
        // The .sha256 file MUST be archived on Jenkins
        TemporaryFile hashFile = artifactFileManager.retrieveArtifact("unsigned.exe.sha256");
        byte[] hashFileContent = TemporaryFileUtil.getContentAndDispose(hashFile);
        String sha256Base64 = new String(hashFileContent, StandardCharsets.UTF_8);

        // Verify the content is a valid base64-encoded SHA-256 hash (44 chars with padding)
        byte[] decodedHash = Base64.getDecoder().decode(sha256Base64);
        assertEquals(32, decodedHash.length);

        // Verify the hash matches the artifact content
        byte[] expectedSha256Bytes = DigestUtils.sha256(unsignedArtifactString.getBytes(StandardCharsets.UTF_8));
        assertArrayEquals(expectedSha256Bytes, decodedHash);

        // The original unsigned artifact must NOT be archived on Jenkins
        try {
            artifactFileManager.retrieveArtifact("unsigned.exe");
            fail("Expected ArtifactNotFoundException: the original artifact should not be uploaded to Jenkins");
        } catch (ArtifactNotFoundException expected) {
            // correct: the artifact is not on Jenkins
        }
    }

    @Theory
    public void submitSigningRequest_withArtifactRetrievalUrl(@FromDataPoints("allBooleans") boolean waitForCompletion) throws Exception {
        byte[] signedArtifactBytes = Some.bytes();
        String trustedBuildSystemTokenCredentialId = Some.stringNonEmpty();
        String trustedBuildSystemToken = Some.stringNonEmpty();
        String unsignedArtifactString = Some.stringNonEmpty();
        String apiTokenCredentialId = Some.stringNonEmpty();
        String apiToken = Some.stringNonEmpty();
        String projectSlug = Some.stringNonEmpty();
        String signingPolicySlug = Some.stringNonEmpty();
        String organizationId = Some.uuid().toString();
        String signingRequestId = Some.uuid().toString();
        String retrievalUrl = getMockUrl("download/artifact.exe");
        String retrievalHeaderName = "Authorization";
        String retrievalHeaderValue = Some.stringNonEmpty();

        CredentialsStore credentialStore = CredentialStoreUtils.getCredentialStore(j.jenkins);
        assert credentialStore != null;
        CredentialStoreUtils.addCredentials(credentialStore, CredentialsScope.SYSTEM, trustedBuildSystemTokenCredentialId, trustedBuildSystemToken);
        CredentialStoreUtils.addCredentials(credentialStore, CredentialsScope.SYSTEM, apiTokenCredentialId, apiToken);

        String apiUrl = getMockUrl();

        stubSubmitWithArtifactRetrievalLink(organizationId, signingRequestId);
        if (waitForCompletion) {
            stubGetSigningRequestCompleted(organizationId, signingRequestId, signedArtifactBytes);
        }

        SignPathPluginGlobalConfiguration globalConfig = GlobalConfiguration.all().get(SignPathPluginGlobalConfiguration.class);
        globalConfig.setApiURL(apiUrl);
        globalConfig.setTrustedBuildSystemCredentialId(trustedBuildSystemTokenCredentialId);

        WorkflowJob workflowJob = createWorkflowJobWithArtifactRetrievalUrl(
                apiUrl, trustedBuildSystemTokenCredentialId, apiTokenCredentialId,
                organizationId, projectSlug, signingPolicySlug, unsignedArtifactString,
                retrievalUrl, retrievalHeaderName, retrievalHeaderValue, waitForCompletion);

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

        if (waitForCompletion) {
            byte[] signedArtifactContent = getSignedArtifactBytes(run);
            assertArrayEquals(signedArtifactBytes, signedArtifactContent);
        }

        String sha256Hex = DigestUtils.sha256Hex(unsignedArtifactString.getBytes(StandardCharsets.UTF_8));
        assertSubmitWithArtifactRetrievalLinkRequest(
                apiToken, trustedBuildSystemToken, organizationId, projectSlug, signingPolicySlug,
                sha256Hex, retrievalUrl, retrievalHeaderName);

        // The header value must NOT appear in the build log (sensitive data)
        assertFalse("Header value must not be logged", run.getLog().contains(retrievalHeaderValue));

        // Neither the old upload-based routes nor the direct upload must have been called
        wireMockRule.verify(exactly(0), postRequestedFor(urlEqualTo("/v1/" + organizationId + "/SigningRequests/SubmitWithoutArtifact")));
        wireMockRule.verify(exactly(0), postRequestedFor(urlPathMatching("/v1/" + organizationId + "/SigningRequests/.*/UploadUnsignedArtifact")));
    }

    @Theory
    public void submitSigningRequest_withRetrievalHttpHeadersButNoUrl_fails() throws Exception {
        WorkflowJob workflowJob = j.createWorkflow("SignPath",
                "writeFile text: 'content', file: 'unsigned.exe'; " +
                        "submitSigningRequest(" +
                        "inputArtifactPath: 'unsigned.exe', " +
                        "trustedBuildSystemTokenCredentialId: '" + Some.stringNonEmpty() + "'," +
                        "apiTokenCredentialId: '" + Some.stringNonEmpty() + "'," +
                        "organizationId: '" + Some.uuid() + "'," +
                        "projectSlug: '" + Some.stringNonEmpty() + "'," +
                        "signingPolicySlug: '" + Some.stringNonEmpty() + "'," +
                        "inputArtifactRetrievalHttpHeaders: [ Authorization: 'Bearer token' ]);");

        BuildData buildData = new BuildData(Some.stringNonEmpty());
        buildData.saveBuild(BuildDataDomainObjectMother.createRandomBuild(1));
        buildData.addRemoteUrl(Some.url());

        // ACT
        QueueTaskFuture<WorkflowRun> runFuture = workflowJob.scheduleBuild2(0, buildData);
        assert runFuture != null;
        WorkflowRun run = runFuture.get();

        // ASSERT
        assertEquals(Result.FAILURE, run.getResult());
        assertTrue(run.getLog().contains("inputArtifactRetrievalHttpHeaders can only be provided together with inputArtifactRetrievalUrl"));
    }

    @Theory
    public void submitSigningRequest_withMissingField_fails() throws Exception {
        WorkflowJob workflowJob = j.createWorkflow("SignPath", "submitSigningRequest();");

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

    @DataPoints("allInvalidRootUrls")
    public static String[] allInvalidRootUrls() {
        return new String[]{"", "not a valid url"};
    }

    @Theory
    public void submitSigningRequest_withWrongOrMissingRootUrl_fails(@FromDataPoints("allInvalidRootUrls") String rootUrl) throws Exception {
        String organizationId = Some.uuid().toString();

        SignPathPluginGlobalConfiguration globalConfig = GlobalConfiguration.all().get(SignPathPluginGlobalConfiguration.class);
        String mockUrl = getMockUrl();
        String tbsToken = Some.stringNonEmpty();
        globalConfig.setApiURL(mockUrl);
        globalConfig.setTrustedBuildSystemCredentialId(tbsToken);

        WorkflowJob workflowJob = createWorkflowJob(mockUrl, tbsToken, Some.stringNonEmpty(), organizationId, Some.stringNonEmpty(), Some.stringNonEmpty(), Some.stringNonEmpty(), false);

        BuildData buildData = new BuildData(Some.stringNonEmpty());
        buildData.saveBuild(BuildDataDomainObjectMother.createRandomBuild(1));
        buildData.addRemoteUrl(Some.url());

        // we set a missing root-url
        JenkinsLocationConfiguration.get().setUrl(rootUrl);

        // ACT
        QueueTaskFuture<WorkflowRun> runFuture = workflowJob.scheduleBuild2(0, buildData);
        assert runFuture != null;
        WorkflowRun run = runFuture.get();

        // ASSERT
        assertEquals(Result.FAILURE, run.getResult());
        assertTrue(run.getLog(), run.getLog().contains("SignPathStepInvalidArgumentException"));
        wireMockRule.verify(exactly(0), postRequestedFor(urlEqualTo("/v1/" + organizationId + "/SigningRequests/SubmitWithoutArtifact")));
    }

    // ---- WireMock stubs ----

    private void stubSubmitWithoutArtifact(String organizationId, String signingRequestId, String uploadLink) {
        wireMockRule.stubFor(post(urlEqualTo("/v1/" + organizationId + "/SigningRequests/SubmitWithoutArtifact"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"signingRequestId\": \"" + signingRequestId + "\", \"uploadLink\": \"" + uploadLink + "\"}")));
    }

    private void stubSubmitWithArtifactRetrievalLink(String organizationId, String signingRequestId) {
        wireMockRule.stubFor(post(urlEqualTo("/v1/" + organizationId + "/SigningRequests/SubmitWithArtifactRetrievalLink"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"signingRequestId\": \"" + signingRequestId + "\", \"webLink\": \"" + getMockUrl("web/" + signingRequestId) + "\"}")));
    }

    private void stubUploadUnsignedArtifact(String uploadPath) {
        wireMockRule.stubFor(post(urlEqualTo(uploadPath))
                .willReturn(aResponse().withStatus(202)));
    }

    private void stubGetSigningRequestCompleted(String organizationId, String signingRequestId, byte[] signedArtifactBytes) {
        String downloadSignedArtifact = "downloadSignedArtifact";

        wireMockRule.stubFor(get(urlEqualTo("/v1/" + organizationId + "/SigningRequests/" + signingRequestId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{status: 'Completed', workflowStatus: 'Completed', isFinalStatus: true, signedArtifactLink: '" + getMockUrl(downloadSignedArtifact) + "'}")));

        wireMockRule.stubFor(get(urlEqualTo("/" + downloadSignedArtifact))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(signedArtifactBytes)));

        wireMockRule.stubFor(get(urlEqualTo("/v1/" + organizationId + "/SigningRequests/" + signingRequestId + "/SignedArtifact"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(signedArtifactBytes)));
    }

    // ---- Pipeline builders ----

    private WorkflowJob createWorkflowJobWithOptionalParameters(String apiUrl,
                                                                String trustedBuildSystemTokenCredentialId,
                                                                String apiTokenCredentialId,
                                                                String organizationId,
                                                                String projectSlug,
                                                                String signingPolicySlug,
                                                                String unsignedArtifactString,
                                                                String artifactConfigurationSlug,
                                                                String description,
                                                                String userDefinedParamName,
                                                                String userDefinedParamValue,
                                                                boolean waitForCompletion) throws IOException {
        return j.createWorkflow("SignPath",
                "writeFile text: '" + unsignedArtifactString + "', file: 'unsigned.exe'; " +
                        "echo '<returnValue>:\"'+ submitSigningRequest( apiUrl: '" + apiUrl + "', " +
                        "inputArtifactPath: 'unsigned.exe', " +
                        "outputArtifactPath: 'signed.exe', " +
                        "trustedBuildSystemTokenCredentialId: '" + trustedBuildSystemTokenCredentialId + "'," +
                        "apiTokenCredentialId: '" + apiTokenCredentialId + "'," +
                        "organizationId: '" + organizationId + "'," +
                        "projectSlug: '" + projectSlug + "'," +
                        "signingPolicySlug: '" + signingPolicySlug + "'," +
                        "artifactConfigurationSlug: '" + artifactConfigurationSlug + "'," +
                        "description: '" + description + "'," +
                        "waitForCompletion: '" + waitForCompletion + "'," +
                        "serviceUnavailableTimeoutInSeconds: 10," +
                        "uploadAndDownloadRequestTimeoutInSeconds: 10," +
                        "parameters: [ " + userDefinedParamName + ": \"" + userDefinedParamValue + "\" ]," +
                        "waitForCompletionTimeoutInSeconds: 10) + '\"';");
    }

    private WorkflowJob createWorkflowJob(String apiUrl,
                                          String trustedBuildSystemTokenCredentialId,
                                          String apiTokenCredentialId,
                                          String organizationId,
                                          String projectSlug,
                                          String signingPolicySlug,
                                          String unsignedArtifactString,
                                          boolean waitForCompletion) throws IOException {
        String outputArtifactPath = waitForCompletion
                ? "outputArtifactPath: 'signed.exe', "
                : "";

        return j.createWorkflow("SignPath",
                "writeFile text: '" + unsignedArtifactString + "', file: 'unsigned.exe'; " +
                        "echo '<returnValue>:\"'+ submitSigningRequest(apiUrl: '" + apiUrl + "', " +
                        "inputArtifactPath: 'unsigned.exe', " +
                        outputArtifactPath +
                        "trustedBuildSystemTokenCredentialId: '" + trustedBuildSystemTokenCredentialId + "'," +
                        "apiTokenCredentialId: '" + apiTokenCredentialId + "'," +
                        "organizationId: '" + organizationId + "'," +
                        "projectSlug: '" + projectSlug + "'," +
                        "signingPolicySlug: '" + signingPolicySlug + "'," +
                        "waitForCompletion: '" + waitForCompletion + "'," +
                        "serviceUnavailableTimeoutInSeconds: 10," +
                        "uploadAndDownloadRequestTimeoutInSeconds: 10," +
                        "waitForCompletionTimeoutInSeconds: 10) + '\"';");
    }

    private WorkflowJob createWorkflowJobWithArtifactRetrievalUrl(String apiUrl,
                                                                   String trustedBuildSystemTokenCredentialId,
                                                                   String apiTokenCredentialId,
                                                                   String organizationId,
                                                                   String projectSlug,
                                                                   String signingPolicySlug,
                                                                   String unsignedArtifactString,
                                                                   String retrievalUrl,
                                                                   String retrievalHeaderName,
                                                                   String retrievalHeaderValue,
                                                                   boolean waitForCompletion) throws IOException {
        String outputArtifactPath = waitForCompletion ? "outputArtifactPath: 'signed.exe', " : "";

        return j.createWorkflow("SignPath",
                "writeFile text: '" + unsignedArtifactString + "', file: 'unsigned.exe'; " +
                        "echo '<returnValue>:\"'+ submitSigningRequest(apiUrl: '" + apiUrl + "', " +
                        "inputArtifactPath: 'unsigned.exe', " +
                        outputArtifactPath +
                        "trustedBuildSystemTokenCredentialId: '" + trustedBuildSystemTokenCredentialId + "'," +
                        "apiTokenCredentialId: '" + apiTokenCredentialId + "'," +
                        "organizationId: '" + organizationId + "'," +
                        "projectSlug: '" + projectSlug + "'," +
                        "signingPolicySlug: '" + signingPolicySlug + "'," +
                        "waitForCompletion: '" + waitForCompletion + "'," +
                        "serviceUnavailableTimeoutInSeconds: 10," +
                        "uploadAndDownloadRequestTimeoutInSeconds: 10," +
                        "waitForCompletionTimeoutInSeconds: 10," +
                        "inputArtifactRetrievalUrl: '" + retrievalUrl + "'," +
                        "inputArtifactRetrievalHttpHeaders: [ " + retrievalHeaderName + ": '" + retrievalHeaderValue + "' ]) + '\"';");
    }

    // ---- Assertions ----

    private void assertSubmitWithoutArtifactRequest(
            String apiToken,
            String trustedBuildSystemToken,
            String unsignedArtifactString,
            String remoteUrl,
            String organizationId,
            String projectSlug,
            String signingPolicySlug) {

        String sha256Hex = DigestUtils.sha256Hex(unsignedArtifactString.getBytes(StandardCharsets.UTF_8));

        wireMockRule.verify(postRequestedFor(urlEqualTo("/v1/" + organizationId + "/SigningRequests/SubmitWithoutArtifact"))
                .withHeader("Authorization", equalTo("Bearer " + apiToken + ":" + trustedBuildSystemToken))
                .withRequestBodyPart(aMultipart().withName("unsignedArtifactMetadata.sha256Hash").withBody(equalTo(sha256Hex)).build())
                .withRequestBodyPart(aMultipart().withName("unsignedArtifactMetadata.fileName").withBody(equalTo("unsigned.exe")).build())
                .withRequestBodyPart(aMultipart().withBody(equalTo(projectSlug)).build())
                .withRequestBodyPart(aMultipart().withBody(equalTo(signingPolicySlug)).build())
                .withRequestBodyPart(aMultipart().withBody(equalTo(remoteUrl)).build()));

        assertBuildSettingsFile(organizationId);
    }

    private void assertSubmitWithoutArtifactRequest(
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

        String sha256Hex = DigestUtils.sha256Hex(unsignedArtifactString.getBytes(StandardCharsets.UTF_8));

        wireMockRule.verify(postRequestedFor(urlEqualTo("/v1/" + organizationId + "/SigningRequests/SubmitWithoutArtifact"))
                .withHeader("Authorization", equalTo("Bearer " + apiToken + ":" + trustedBuildSystemToken))
                .withRequestBodyPart(aMultipart().withName("unsignedArtifactMetadata.sha256Hash").withBody(equalTo(sha256Hex)).build())
                .withRequestBodyPart(aMultipart().withName("unsignedArtifactMetadata.fileName").withBody(equalTo("unsigned.exe")).build())
                .withRequestBodyPart(aMultipart().withBody(equalTo(projectSlug)).build())
                .withRequestBodyPart(aMultipart().withBody(equalTo(signingPolicySlug)).build())
                .withRequestBodyPart(aMultipart().withBody(equalTo(remoteUrl)).build())
                .withRequestBodyPart(aMultipart().withBody(equalTo(description)).build())
                .withRequestBodyPart(aMultipart().withBody(equalTo(artifactConfigurationSlug)).build())
                .withRequestBodyPart(aMultipart().withBody(equalTo(userDefinedParamValue)).build())
                .withRequestBodyPart(aMultipart().withName("Parameters." + userDefinedParamName).build()));

        assertBuildSettingsFile(organizationId);
    }

    private void assertSubmitWithArtifactRetrievalLinkRequest(
            String apiToken,
            String trustedBuildSystemToken,
            String organizationId,
            String projectSlug,
            String signingPolicySlug,
            String sha256Hex,
            String retrievalUrl,
            String retrievalHeaderName) {

        wireMockRule.verify(postRequestedFor(urlEqualTo("/v1/" + organizationId + "/SigningRequests/SubmitWithArtifactRetrievalLink"))
                .withHeader("Authorization", equalTo("Bearer " + apiToken + ":" + trustedBuildSystemToken))
                .withRequestBodyPart(aMultipart().withName("ArtifactRetrievalLink.FileName").withBody(equalTo("unsigned.exe")).build())
                .withRequestBodyPart(aMultipart().withName("ArtifactRetrievalLink.Sha256Hash").withBody(equalTo(sha256Hex)).build())
                .withRequestBodyPart(aMultipart().withName("ArtifactRetrievalLink.Url").withBody(equalTo(retrievalUrl)).build())
                .withRequestBodyPart(aMultipart().withName("ArtifactRetrievalLink.HttpHeaders[0].Key").withBody(equalTo(retrievalHeaderName)).build())
                .withRequestBodyPart(aMultipart().withBody(equalTo(projectSlug)).build())
                .withRequestBodyPart(aMultipart().withBody(equalTo(signingPolicySlug)).build()));

        Request r = wireMockRule.findAll(postRequestedFor(urlEqualTo("/v1/" + organizationId + "/SigningRequests/SubmitWithArtifactRetrievalLink"))).get(0);
        String buildSettingsFile = getMultipartFormDataFileContents(r, "Origin.BuildData.BuildSettingsFile");
        assertTrue(buildSettingsFile.contains("submitSigningRequest"));
        assertTrue(buildSettingsFile.contains(organizationId));
    }

    private void assertUploadRequest(String uploadPath, String unsignedArtifactString) {
        wireMockRule.verify(postRequestedFor(urlEqualTo(uploadPath))
                .withRequestBody(equalTo(unsignedArtifactString)));
    }

    private void assertBuildSettingsFile(String organizationId) {
        Request r = wireMockRule.findAll(postRequestedFor(urlEqualTo("/v1/" + organizationId + "/SigningRequests/SubmitWithoutArtifact"))).get(0);
        String buildSettingsFile = getMultipartFormDataFileContents(r, "Origin.BuildData.BuildSettingsFile");
        assertTrue(buildSettingsFile.contains("node {writeFile text:"));
        assertTrue(buildSettingsFile.contains(organizationId));
    }

    private String getMultipartFormDataFileContents(Request r, String name) {
        for (Request.Part part : r.getParts()) {
            if (name.equals(part.getName())) {
                return part.getBody().asString();
            }
        }
        fail("multipart-form-data with name " + name + " not found in " + r.getBodyAsString());
        return null;
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

    @DataPoints("allBooleans")
    public static boolean[] allBooleans() {
        return new boolean[]{true, false};
    }
}
