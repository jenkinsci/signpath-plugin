package io.jenkins.plugins.signpath;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.FingerprintMap;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.model.queue.QueueTaskFuture;
import io.jenkins.plugins.signpath.Artifacts.DefaultArtifactFileManager;
import io.jenkins.plugins.signpath.Common.TemporaryFile;
import io.jenkins.plugins.signpath.Exceptions.ArtifactNotFoundException;
import io.jenkins.plugins.signpath.TestUtils.*;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.FromDataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import jenkins.model.GlobalConfiguration;
import static org.junit.Assert.*;

@RunWith(Theories.class)
public class SubmitSigningRequestStepEndToEndTest {
    private static final int MockServerPort = 51000;
    private static final String UNSIGNED_ARTIFACT_PATH = "unsigned.exe";
    private static final String SIGNED_ARTIFACT_PATH = "signed.exe";
    private static final String SHA256_ARTIFACT_PATH = UNSIGNED_ARTIFACT_PATH + ".sha256";
    private static final String ENDPOINT_SLUG = "JenkinsOnPrem";
    private static final String ARTIFACT_UPLOAD_LINK = "upload-link-abc123";
    // The workflow job is always created with this name, so run.getParent().getFullName() is deterministic.
    private static final String JOB_FULL_NAME = "SignPath";

    @Rule
    public final SignPathJenkinsRule j = new SignPathJenkinsRule();

    @Rule
    public final WireMockRule wireMockRule = new WireMockRule(MockServerPort);

    @Theory
    public void submitSigningRequest(@FromDataPoints("allBooleans") boolean withOptionalFields) throws Exception {
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
        CredentialStoreUtils.addCredentials(credentialStore, CredentialsScope.SYSTEM, apiTokenCredentialId, apiToken);

        stubConnectorSubmit(organizationId, signingRequestId);
        stubConnectorUnsignedArtifactUpload(organizationId, signingRequestId);
        stubConnectorStatus(organizationId, signingRequestId);

        configureGlobalConfig();

        WorkflowJob workflowJob = withOptionalFields
                ? createWorkflowJobWithOptionalParameters(apiTokenCredentialId, organizationId, projectSlug, signingPolicySlug, unsignedArtifactString, artifactConfigurationSlug, description, userDefinedParamName, userDefinedParamValue, true)
                : createWorkflowJob(apiTokenCredentialId, organizationId, projectSlug, signingPolicySlug, unsignedArtifactString, true);

        // ACT
        QueueTaskFuture<WorkflowRun> runFuture = workflowJob.scheduleBuild2(0);
        assert runFuture != null;
        WorkflowRun run = runFuture.get();

        // ASSERT
        assertSuccess(run);
        assertTrue(run.getLog().contains("<returnValue>:\"" + signingRequestId + "\""));

        // Only the .sha256 sidecar is archived; the unsigned artifact is uploaded directly to the connector.
        assertArtifactNotArchived(run, UNSIGNED_ARTIFACT_PATH);
        assertArtifactArchived(run, SHA256_ARTIFACT_PATH);
        // Signed artifact must NOT be archived automatically
        assertArtifactNotArchived(run, SIGNED_ARTIFACT_PATH);

        // Status endpoint must have been polled (waitForCompletion was true)
        wireMockRule.verify(getRequestedFor(urlEqualTo(statusRoute(organizationId, signingRequestId))));

        assertConnectorSubmitRequest(apiToken, organizationId, projectSlug, signingPolicySlug);
        // The unsigned artifact must be uploaded in a follow-up request using the returned upload link
        assertUnsignedArtifactUploaded(apiToken, organizationId, signingRequestId, unsignedArtifactString);
        if (withOptionalFields) {
            wireMockRule.verify(postRequestedFor(urlEqualTo(submitRoute(organizationId)))
                    .withRequestBody(containing("\"SignPathArtifactConfigurationSlug\":\"" + artifactConfigurationSlug + "\""))
                    .withRequestBody(containing("\"SigningRequestDescription\":\"" + description + "\""))
                    .withRequestBody(containing("\"Name\":\"" + userDefinedParamName + "\""))
                    .withRequestBody(containing("\"Value\":\"" + userDefinedParamValue + "\"")));
        }
    }

    @Theory
    public void submitSigningRequest_withOutputArtifactPath() throws Exception {
        byte[] signedArtifactBytes = Some.bytes();
        String unsignedArtifactString = Some.stringNonEmpty();
        String apiTokenCredentialId = Some.stringNonEmpty();
        String apiToken = Some.stringNonEmpty();
        String projectSlug = Some.stringNonEmpty();
        String signingPolicySlug = Some.stringNonEmpty();
        String organizationId = Some.uuid().toString();
        String signingRequestId = Some.uuid().toString();

        CredentialsStore credentialStore = CredentialStoreUtils.getCredentialStore(j.jenkins);
        assert credentialStore != null;
        CredentialStoreUtils.addCredentials(credentialStore, CredentialsScope.SYSTEM, apiTokenCredentialId, apiToken);

        stubConnectorSubmit(organizationId, signingRequestId);
        stubConnectorUnsignedArtifactUpload(organizationId, signingRequestId);
        stubConnectorStatus(organizationId, signingRequestId);
        stubConnectorSignedArtifact(organizationId, signingRequestId, signedArtifactBytes);

        configureGlobalConfig();

        WorkflowJob workflowJob = j.createWorkflow("SignPath",
                "writeFile text: '" + unsignedArtifactString + "', file: '" + UNSIGNED_ARTIFACT_PATH + "'; " +
                        "echo '<returnValue>:\"'+ submitSigningRequest(" +
                        "inputArtifactPath: '" + UNSIGNED_ARTIFACT_PATH + "', " +
                        "apiTokenCredentialId: '" + apiTokenCredentialId + "'," +
                        "organizationId: '" + organizationId + "'," +
                        "projectSlug: '" + projectSlug + "'," +
                        "signingPolicySlug: '" + signingPolicySlug + "'," +
                        "waitForCompletion: true," +
                        "outputArtifactPath: '" + SIGNED_ARTIFACT_PATH + "'," +
                        "serviceUnavailableTimeoutInSeconds: 10," +
                        "uploadAndDownloadRequestTimeoutInSeconds: 10," +
                        "waitForCompletionTimeoutInSeconds: 10) + '\"';");

        // ACT
        QueueTaskFuture<WorkflowRun> runFuture = workflowJob.scheduleBuild2(0);
        assert runFuture != null;
        WorkflowRun run = runFuture.get();

        // ASSERT
        assertSuccess(run);
        assertTrue(run.getLog().contains("<returnValue>:\"" + signingRequestId + "\""));

        // Signed artifact must be stored at the specified outputArtifactPath
        byte[] actualSignedArtifactBytes = getSignedArtifactBytes(workflowJob);
        assertArrayEquals(signedArtifactBytes, actualSignedArtifactBytes);

        // Status and download endpoints must both have been called
        wireMockRule.verify(getRequestedFor(urlEqualTo(statusRoute(organizationId, signingRequestId))));
        wireMockRule.verify(getRequestedFor(urlEqualTo(signedArtifactRoute(organizationId, signingRequestId))));
        assertUnsignedArtifactUploaded(apiToken, organizationId, signingRequestId, unsignedArtifactString);
    }

    @Theory
    public void submitSigningRequest_withoutWaitForCompletion(@FromDataPoints("allBooleans") boolean withOptionalFields) throws Exception {
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
        CredentialStoreUtils.addCredentials(credentialStore, CredentialsScope.SYSTEM, apiTokenCredentialId, apiToken);

        stubConnectorSubmit(organizationId, signingRequestId);
        stubConnectorUnsignedArtifactUpload(organizationId, signingRequestId);

        configureGlobalConfig();

        WorkflowJob workflowJob = withOptionalFields
                ? createWorkflowJobWithOptionalParameters(apiTokenCredentialId, organizationId, projectSlug, signingPolicySlug, unsignedArtifactString, artifactConfigurationSlug, description, userDefinedParamName, userDefinedParamValue, false)
                : createWorkflowJob(apiTokenCredentialId, organizationId, projectSlug, signingPolicySlug, unsignedArtifactString, false);

        // ACT
        QueueTaskFuture<WorkflowRun> runFuture = workflowJob.scheduleBuild2(0);
        assert runFuture != null;
        WorkflowRun run = runFuture.get();

        // ASSERT
        assertSuccess(run);
        assertTrue(run.getLog().contains("<returnValue>:\"" + signingRequestId + "\""));

        assertConnectorSubmitRequest(apiToken, organizationId, projectSlug, signingPolicySlug);
        assertUnsignedArtifactUploaded(apiToken, organizationId, signingRequestId, unsignedArtifactString);
        // Status endpoint must NOT have been polled (waitForCompletion was false)
        wireMockRule.verify(exactly(0), getRequestedFor(urlEqualTo(statusRoute(organizationId, signingRequestId))));
    }

    @Theory
    public void submitSigningRequest_onlySidecarIsArchived() throws Exception {
        String unsignedArtifactString = Some.stringNonEmpty();
        String apiTokenCredentialId = Some.stringNonEmpty();
        String apiToken = Some.stringNonEmpty();
        String organizationId = Some.uuid().toString();
        String signingRequestId = Some.uuid().toString();

        CredentialsStore credentialStore = CredentialStoreUtils.getCredentialStore(j.jenkins);
        assert credentialStore != null;
        CredentialStoreUtils.addCredentials(credentialStore, CredentialsScope.SYSTEM, apiTokenCredentialId, apiToken);

        stubConnectorSubmit(organizationId, signingRequestId);
        stubConnectorUnsignedArtifactUpload(organizationId, signingRequestId);

        configureGlobalConfig();

        WorkflowJob workflowJob = createWorkflowJob(apiTokenCredentialId,
                organizationId, Some.stringNonEmpty(), Some.stringNonEmpty(), unsignedArtifactString, false);

        // ACT
        QueueTaskFuture<WorkflowRun> runFuture = workflowJob.scheduleBuild2(0);
        assert runFuture != null;
        WorkflowRun run = runFuture.get();

        // ASSERT
        assertSuccess(run);

        Launcher launcher = j.createLocalLauncher();
        TaskListener listener = j.createTaskListener();
        FingerprintMap fingerprintMap = j.jenkins.getFingerprintMap();
        DefaultArtifactFileManager artifactFileManager = new DefaultArtifactFileManager(fingerprintMap, run, launcher, listener);

        // The .sha256 sidecar MUST be archived on Jenkins and contain the base64-encoded SHA-256 of the artifact
        TemporaryFile hashFile = artifactFileManager.retrieveArtifact(SHA256_ARTIFACT_PATH);
        byte[] hashFileContent = TemporaryFileUtil.getContentAndDispose(hashFile);
        String sha256Base64 = new String(hashFileContent, StandardCharsets.UTF_8);
        byte[] decodedHash = Base64.getDecoder().decode(sha256Base64);
        assertEquals(32, decodedHash.length);
        byte[] expectedSha256Bytes = DigestUtils.sha256(unsignedArtifactString.getBytes(StandardCharsets.UTF_8));
        assertArrayEquals(expectedSha256Bytes, decodedHash);

        // The unsigned artifact itself MUST NOT be archived (it is uploaded to the connector directly).
        assertArtifactNotArchived(run, UNSIGNED_ARTIFACT_PATH);
    }

    @Theory
    public void submitSigningRequest_withArtifactRetrievalUrl(@FromDataPoints("allBooleans") boolean waitForCompletion) throws Exception {
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
        CredentialStoreUtils.addCredentials(credentialStore, CredentialsScope.SYSTEM, apiTokenCredentialId, apiToken);

        // No upload link is returned when SignPath retrieves the artifact itself
        stubConnectorSubmit(organizationId, signingRequestId, null);
        if (waitForCompletion) {
            stubConnectorStatus(organizationId, signingRequestId);
        }

        configureGlobalConfig();

        WorkflowJob workflowJob = createWorkflowJobWithArtifactRetrievalUrl(
                apiTokenCredentialId, organizationId, projectSlug, signingPolicySlug, unsignedArtifactString,
                retrievalUrl, retrievalHeaderName, retrievalHeaderValue, waitForCompletion);

        // ACT
        QueueTaskFuture<WorkflowRun> runFuture = workflowJob.scheduleBuild2(0);
        assert runFuture != null;
        WorkflowRun run = runFuture.get();

        // ASSERT
        assertSuccess(run);
        assertTrue(run.getLog().contains("<returnValue>:\"" + signingRequestId + "\""));

        if (waitForCompletion) {
            assertArtifactNotArchived(run, SIGNED_ARTIFACT_PATH);
            wireMockRule.verify(getRequestedFor(urlEqualTo(statusRoute(organizationId, signingRequestId))));
        }

        // In retrieval-URL mode only the sidecar is archived; the artifact itself is not.
        assertArtifactArchived(run, SHA256_ARTIFACT_PATH);
        assertArtifactNotArchived(run, UNSIGNED_ARTIFACT_PATH);

        wireMockRule.verify(postRequestedFor(urlEqualTo(submitRoute(organizationId)))
                .withHeader("Authorization", equalTo("Bearer " + apiToken))
                .withRequestBody(containing("\"InputArtifactRetrievalUrl\":\"" + retrievalUrl + "\""))
                .withRequestBody(containing("\"InputArtifactRetrievalHttpHeaders\"")));

        // The artifact is retrieved by SignPath, so it must not be uploaded by the plugin
        wireMockRule.verify(exactly(0), postRequestedFor(urlPathEqualTo(unsignedArtifactRoute(organizationId, signingRequestId))));

        // The header value must NOT appear in the build log (sensitive data)
        assertFalse("Header value must not be logged", run.getLog().contains(retrievalHeaderValue));
    }

    @Theory
    public void submitSigningRequest_withoutArtifactUploadLink_fails() throws Exception {
        String unsignedArtifactString = Some.stringNonEmpty();
        String apiTokenCredentialId = Some.stringNonEmpty();
        String apiToken = Some.stringNonEmpty();
        String organizationId = Some.uuid().toString();
        String signingRequestId = Some.uuid().toString();

        CredentialsStore credentialStore = CredentialStoreUtils.getCredentialStore(j.jenkins);
        assert credentialStore != null;
        CredentialStoreUtils.addCredentials(credentialStore, CredentialsScope.SYSTEM, apiTokenCredentialId, apiToken);

        // The connector does not return an upload link although the plugin has to upload the artifact
        stubConnectorSubmit(organizationId, signingRequestId, null);

        configureGlobalConfig();

        WorkflowJob workflowJob = createWorkflowJob(apiTokenCredentialId, organizationId,
                Some.stringNonEmpty(), Some.stringNonEmpty(), unsignedArtifactString, false);

        // ACT
        QueueTaskFuture<WorkflowRun> runFuture = workflowJob.scheduleBuild2(0);
        assert runFuture != null;
        WorkflowRun run = runFuture.get();

        // ASSERT
        assertEquals(Result.FAILURE, run.getResult());
        assertTrue(run.getLog().contains("The connector did not return an artifact upload link"));
        wireMockRule.verify(exactly(0), postRequestedFor(urlPathEqualTo(unsignedArtifactRoute(organizationId, signingRequestId))));
    }

    @Theory
    public void submitSigningRequest_withOutputArtifactPathButNoWaitForCompletion_fails() throws Exception {
        configureGlobalConfig();

        WorkflowJob workflowJob = j.createWorkflow("SignPath",
                "writeFile text: 'content', file: '" + UNSIGNED_ARTIFACT_PATH + "'; " +
                        "submitSigningRequest(" +
                        "inputArtifactPath: '" + UNSIGNED_ARTIFACT_PATH + "', " +
                        "apiTokenCredentialId: '" + Some.stringNonEmpty() + "'," +
                        "organizationId: '" + Some.uuid() + "'," +
                        "projectSlug: '" + Some.stringNonEmpty() + "'," +
                        "signingPolicySlug: '" + Some.stringNonEmpty() + "'," +
                        "waitForCompletion: false," +
                        "outputArtifactPath: '" + SIGNED_ARTIFACT_PATH + "');");

        // ACT
        QueueTaskFuture<WorkflowRun> runFuture = workflowJob.scheduleBuild2(0);
        assert runFuture != null;
        WorkflowRun run = runFuture.get();

        // ASSERT
        assertEquals(Result.FAILURE, run.getResult());
        assertTrue(run.getLog().contains("outputArtifactPath can only be set if waitForCompletion is true"));
    }

    @Theory
    public void submitSigningRequest_withRetrievalHttpHeadersButNoUrl_fails() throws Exception {
        configureGlobalConfig();

        WorkflowJob workflowJob = j.createWorkflow("SignPath",
                "writeFile text: 'content', file: '" + UNSIGNED_ARTIFACT_PATH + "'; " +
                        "submitSigningRequest(" +
                        "inputArtifactPath: '" + UNSIGNED_ARTIFACT_PATH + "', " +
                        "apiTokenCredentialId: '" + Some.stringNonEmpty() + "'," +
                        "organizationId: '" + Some.uuid() + "'," +
                        "projectSlug: '" + Some.stringNonEmpty() + "'," +
                        "signingPolicySlug: '" + Some.stringNonEmpty() + "'," +
                        "inputArtifactRetrievalHttpHeaders: [ Authorization: 'Bearer token' ]);");

        // ACT
        QueueTaskFuture<WorkflowRun> runFuture = workflowJob.scheduleBuild2(0);
        assert runFuture != null;
        WorkflowRun run = runFuture.get();

        // ASSERT
        assertEquals(Result.FAILURE, run.getResult());
        assertTrue(run.getLog().contains("inputArtifactRetrievalHttpHeaders can only be provided together with inputArtifactRetrievalUrl"));
    }

    @Theory
    public void submitSigningRequest_withMissingField_fails() throws Exception {
        configureGlobalConfig();

        WorkflowJob workflowJob = j.createWorkflow("SignPath", "submitSigningRequest();");

        // ACT
        QueueTaskFuture<WorkflowRun> runFuture = workflowJob.scheduleBuild2(0);
        assert runFuture != null;
        WorkflowRun run = runFuture.get();

        // ASSERT
        assertEquals(Result.FAILURE, run.getResult());
        assertTrue(run.getLog().contains("SignPathStepInvalidArgumentException"));
    }

    @Theory
    public void submitSigningRequest_withMissingConnectorEndpointSlug_fails() throws Exception {
        String apiTokenCredentialId = Some.stringNonEmpty();
        String apiToken = Some.stringNonEmpty();
        CredentialsStore credentialStore = CredentialStoreUtils.getCredentialStore(j.jenkins);
        assert credentialStore != null;
        CredentialStoreUtils.addCredentials(credentialStore, CredentialsScope.SYSTEM, apiTokenCredentialId, apiToken);

        // Connector URL configured but no endpoint slug (globally or at the step level)
        SignPathPluginGlobalConfiguration globalConfig = GlobalConfiguration.all().get(SignPathPluginGlobalConfiguration.class);
        globalConfig.setConnectorURL(getMockUrl());

        WorkflowJob workflowJob = createWorkflowJob(apiTokenCredentialId, Some.uuid().toString(),
                Some.stringNonEmpty(), Some.stringNonEmpty(), Some.stringNonEmpty(), false);

        // ACT
        QueueTaskFuture<WorkflowRun> runFuture = workflowJob.scheduleBuild2(0);
        assert runFuture != null;
        WorkflowRun run = runFuture.get();

        // ASSERT
        assertEquals(Result.FAILURE, run.getResult());
        assertTrue(run.getLog().contains("connectorEndpointSlug"));
    }

    // ---- WireMock stubs ----

    private void stubConnectorSubmit(String organizationId, String signingRequestId) {
        stubConnectorSubmit(organizationId, signingRequestId, ARTIFACT_UPLOAD_LINK);
    }

    private void stubConnectorSubmit(String organizationId, String signingRequestId, String artifactUploadLink) {
        String artifactUploadLinkJson = artifactUploadLink == null
                ? ""
                : ", \"artifactUploadLink\": \"" + artifactUploadLink + "\"";
        wireMockRule.stubFor(post(urlEqualTo(submitRoute(organizationId)))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"signingRequestId\": \"" + signingRequestId + "\", \"signingRequestUrl\": \""
                                + getMockUrl("web/" + signingRequestId) + "\"" + artifactUploadLinkJson + ", \"logs\": []}")));
    }

    private void stubConnectorUnsignedArtifactUpload(String organizationId, String signingRequestId) {
        wireMockRule.stubFor(post(urlPathEqualTo(unsignedArtifactRoute(organizationId, signingRequestId)))
                .willReturn(aResponse().withStatus(200)));
    }

    private void stubConnectorStatus(String organizationId, String signingRequestId) {
        wireMockRule.stubFor(get(urlEqualTo(statusRoute(organizationId, signingRequestId)))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{\"status\": \"Completed\", \"isFinalStatus\": true, \"webLink\": \"https://app.signpath.io/sr\"}")));
    }

    private void stubConnectorSignedArtifact(String organizationId, String signingRequestId, byte[] signedArtifactBytes) {
        wireMockRule.stubFor(get(urlEqualTo(signedArtifactRoute(organizationId, signingRequestId)))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(signedArtifactBytes)));
    }

    // ---- Routes ----

    private static String submitRoute(String organizationId) {
        return "/Jenkins/" + ENDPOINT_SLUG + "/" + organizationId + "/SigningRequests";
    }

    private static String statusRoute(String organizationId, String signingRequestId) {
        return submitRoute(organizationId) + "/" + signingRequestId + "/Status";
    }

    private static String signedArtifactRoute(String organizationId, String signingRequestId) {
        return submitRoute(organizationId) + "/" + signingRequestId + "/SignedArtifact";
    }

    private static String unsignedArtifactRoute(String organizationId, String signingRequestId) {
        return submitRoute(organizationId) + "/" + signingRequestId + "/UnsignedArtifact";
    }

    // ---- Pipeline builders ----

    private void configureGlobalConfig() {
        SignPathPluginGlobalConfiguration globalConfig = GlobalConfiguration.all().get(SignPathPluginGlobalConfiguration.class);
        globalConfig.setConnectorURL(getMockUrl());
        globalConfig.setConnectorEndpointSlug(ENDPOINT_SLUG);
    }

    private WorkflowJob createWorkflowJobWithOptionalParameters(String apiTokenCredentialId,
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
                "writeFile text: '" + unsignedArtifactString + "', file: '" + UNSIGNED_ARTIFACT_PATH + "'; " +
                        "echo '<returnValue>:\"'+ submitSigningRequest(" +
                        "inputArtifactPath: '" + UNSIGNED_ARTIFACT_PATH + "', " +
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

    private WorkflowJob createWorkflowJob(String apiTokenCredentialId,
                                          String organizationId,
                                          String projectSlug,
                                          String signingPolicySlug,
                                          String unsignedArtifactString,
                                          boolean waitForCompletion) throws IOException {
        return j.createWorkflow("SignPath",
                "writeFile text: '" + unsignedArtifactString + "', file: '" + UNSIGNED_ARTIFACT_PATH + "'; " +
                        "echo '<returnValue>:\"'+ submitSigningRequest(" +
                        "inputArtifactPath: '" + UNSIGNED_ARTIFACT_PATH + "', " +
                        "apiTokenCredentialId: '" + apiTokenCredentialId + "'," +
                        "organizationId: '" + organizationId + "'," +
                        "projectSlug: '" + projectSlug + "'," +
                        "signingPolicySlug: '" + signingPolicySlug + "'," +
                        "waitForCompletion: '" + waitForCompletion + "'," +
                        "serviceUnavailableTimeoutInSeconds: 10," +
                        "uploadAndDownloadRequestTimeoutInSeconds: 10," +
                        "waitForCompletionTimeoutInSeconds: 10) + '\"';");
    }

    private WorkflowJob createWorkflowJobWithArtifactRetrievalUrl(String apiTokenCredentialId,
                                                                   String organizationId,
                                                                   String projectSlug,
                                                                   String signingPolicySlug,
                                                                   String unsignedArtifactString,
                                                                   String retrievalUrl,
                                                                   String retrievalHeaderName,
                                                                   String retrievalHeaderValue,
                                                                   boolean waitForCompletion) throws IOException {
        return j.createWorkflow("SignPath",
                "writeFile text: '" + unsignedArtifactString + "', file: '" + UNSIGNED_ARTIFACT_PATH + "'; " +
                        "echo '<returnValue>:\"'+ submitSigningRequest(" +
                        "inputArtifactPath: '" + UNSIGNED_ARTIFACT_PATH + "', " +
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

    private void assertSuccess(WorkflowRun run) throws IOException {
        if (run.getResult() != Result.SUCCESS) {
            assertEquals("", run.getLog() + run.getResult());
            fail();
        }
    }

    private void assertConnectorSubmitRequest(String apiToken, String organizationId, String projectSlug, String signingPolicySlug) {
        wireMockRule.verify(postRequestedFor(urlEqualTo(submitRoute(organizationId)))
                .withHeader("Authorization", equalTo("Bearer " + apiToken))
                .withRequestBody(containing("\"JobFullName\":\"" + JOB_FULL_NAME + "\""))
                .withRequestBody(containing("\"BuildNumber\":1"))
                .withRequestBody(containing("\"Sha256ArtifactPath\":\"" + SHA256_ARTIFACT_PATH + "\""))
                .withRequestBody(containing("\"SignPathProjectSlug\":\"" + projectSlug + "\""))
                .withRequestBody(containing("\"SignPathSigningPolicySlug\":\"" + signingPolicySlug + "\"")));
    }

    private void assertUnsignedArtifactUploaded(String apiToken, String organizationId, String signingRequestId, String expectedContent) {
        wireMockRule.verify(postRequestedFor(urlPathEqualTo(unsignedArtifactRoute(organizationId, signingRequestId)))
                .withHeader("Authorization", equalTo("Bearer " + apiToken))
                .withQueryParam("uploadLink", equalTo(ARTIFACT_UPLOAD_LINK))
                .withRequestBody(equalTo(expectedContent)));
    }

    private byte[] getSignedArtifactBytes(WorkflowJob workflowJob) throws IOException, InterruptedException {
        FilePath workspace = j.jenkins.getWorkspaceFor(workflowJob);
        assert workspace != null;
        try (InputStream in = workspace.child(SIGNED_ARTIFACT_PATH).read()) {
            return in.readAllBytes();
        }
    }

    private void assertArtifactArchived(WorkflowRun run, String artifactPath) throws IOException {
        DefaultArtifactFileManager artifactFileManager = newArtifactFileManager(run);
        try {
            TemporaryFile file = artifactFileManager.retrieveArtifact(artifactPath);
            TemporaryFileUtil.getContentAndDispose(file);
        } catch (ArtifactNotFoundException ex) {
            fail("Expected artifact '" + artifactPath + "' to be archived on Jenkins, but it was not found.");
        }
    }

    private void assertArtifactNotArchived(WorkflowRun run, String artifactPath) throws IOException {
        DefaultArtifactFileManager artifactFileManager = newArtifactFileManager(run);
        try {
            artifactFileManager.retrieveArtifact(artifactPath);
            fail("Expected artifact '" + artifactPath + "' NOT to be archived on Jenkins, but it was found.");
        } catch (ArtifactNotFoundException expected) {
            // correct behavior
        }
    }

    private DefaultArtifactFileManager newArtifactFileManager(WorkflowRun run) throws IOException {
        Launcher launcher = j.createLocalLauncher();
        TaskListener listener = j.createTaskListener();
        FingerprintMap fingerprintMap = j.jenkins.getFingerprintMap();
        return new DefaultArtifactFileManager(fingerprintMap, run, launcher, listener);
    }

    private String getMockUrl() {
        return getMockUrl("");
    }

    private String getMockUrl(String postfix) {
        return String.format("http://localhost:%d/%s", MockServerPort, postfix);
    }

    @DataPoints("allBooleans")
    public static boolean[] allBooleans() {
        return new boolean[]{true, false};
    }
}
