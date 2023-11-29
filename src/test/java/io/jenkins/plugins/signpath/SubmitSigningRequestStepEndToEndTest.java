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

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
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

        CredentialsStore credentialStore = CredentialStoreUtils.getCredentialStore(j.jenkins);
        assert credentialStore != null;
        CredentialStoreUtils.addCredentials(credentialStore, CredentialsScope.SYSTEM, trustedBuildSystemTokenCredentialId, trustedBuildSystemToken);
        CredentialStoreUtils.addCredentials(credentialStore, CredentialsScope.SYSTEM, apiTokenCredentialId, apiToken);

        String apiUrl = getMockUrl();
        String downloadSignedArtifact = "downloadSignedArtifact";

        wireMockRule.stubFor(post(urlEqualTo("/v1/" + organizationId + "/SigningRequests"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{signingRequestId: '" + signingRequestId + "'}")
                        .withHeader("Location", getMockUrl("v1/" + organizationId + "/SigningRequests/" + signingRequestId))));

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
 
        WorkflowJob workflowJob = withOptionalFields
                ? createWorkflowJobWithOptionalParameters(apiUrl, trustedBuildSystemTokenCredentialId, apiTokenCredentialId, organizationId, projectSlug, signingPolicySlug, unsignedArtifactString, artifactConfigurationSlug, description, true)
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

        if (withOptionalFields)
            assertRequest(apiToken, trustedBuildSystemToken, unsignedArtifactString, remoteUrl, organizationId, projectSlug, signingPolicySlug, artifactConfigurationSlug, description);
        else
            assertRequest(apiToken, trustedBuildSystemToken, unsignedArtifactString, remoteUrl, organizationId, projectSlug, signingPolicySlug);
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

        CredentialsStore credentialStore = CredentialStoreUtils.getCredentialStore(j.jenkins);
        assert credentialStore != null;
        CredentialStoreUtils.addCredentials(credentialStore, CredentialsScope.SYSTEM, trustedBuildSystemTokenCredentialId, trustedBuildSystemToken);
        CredentialStoreUtils.addCredentials(credentialStore, CredentialsScope.SYSTEM, apiTokenCredentialId, apiToken);

        String apiUrl = getMockUrl();
        wireMockRule.stubFor(post(urlEqualTo("/v1/" + organizationId + "/SigningRequests"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{signingRequestId: '" + signingRequestId + "'}")
                        .withHeader("Location", getMockUrl("v1/" + organizationId + "/SigningRequests/" + signingRequestId))));

        WorkflowJob workflowJob = withOptionalFields
                ? createWorkflowJobWithOptionalParameters(apiUrl, trustedBuildSystemTokenCredentialId, apiTokenCredentialId, organizationId, projectSlug, signingPolicySlug, unsignedArtifactString, artifactConfigurationSlug, description, false)
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

        if (withOptionalFields)
            assertRequest(apiToken, trustedBuildSystemToken, unsignedArtifactString, remoteUrl, organizationId, projectSlug, signingPolicySlug, artifactConfigurationSlug, description);
        else
            assertRequest(apiToken, trustedBuildSystemToken, unsignedArtifactString, remoteUrl, organizationId, projectSlug, signingPolicySlug);
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

        WorkflowJob workflowJob = createWorkflowJob(getMockUrl(), Some.stringNonEmpty(), Some.stringNonEmpty(), organizationId, Some.stringNonEmpty(), Some.stringNonEmpty(), Some.stringNonEmpty(), false);

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
        wireMockRule.verify(exactly(0), postRequestedFor(urlEqualTo("/v1/" + organizationId + "/SigningRequests")));
    }

    private WorkflowJob createWorkflowJobWithOptionalParameters(String apiUrl,
                                                                String trustedBuildSystemTokenCredentialId,
                                                                String apiTokenCredentialId,
                                                                String organizationId,
                                                                String projectSlug,
                                                                String signingPolicySlug,
                                                                String unsignedArtifactString,
                                                                String artifactConfigurationSlug,
                                                                String description,
                                                                boolean waitForCompletion) throws IOException {
        return j.createWorkflow("SignPath",
                "writeFile text: '" + unsignedArtifactString + "', file: 'unsigned.exe'; " +
                        "archiveArtifacts artifacts: 'unsigned.exe', fingerprint: true; " +
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
                        "archiveArtifacts artifacts: 'unsigned.exe', fingerprint: true; " +
                        "echo '<returnValue>:\"'+ submitSigningRequest( apiUrl: '" + apiUrl + "', " +
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

    private void assertRequest(
            String apiToken,
            String trustedBuildSystemToken,
            String unsignedArtifactString,
            String remoteUrl,
            String organizationId,
            String projectSlug,
            String signingPolicySlug) {

        wireMockRule.verify(postRequestedFor(urlEqualTo("/v1/" + organizationId + "/SigningRequests"))
                .withHeader("Authorization", equalTo("Bearer " + apiToken + ":" + trustedBuildSystemToken))
                .withRequestBodyPart(aMultipart().withBody(equalTo(projectSlug)).build())
                .withRequestBodyPart(aMultipart().withBody(equalTo(signingPolicySlug)).build())
                .withRequestBodyPart(aMultipart().withBody(equalTo(remoteUrl)).build()));

        assertFormFiles(unsignedArtifactString, organizationId);
    }

    private void assertRequest(
            String apiToken,
            String trustedBuildSystemToken,
            String unsignedArtifactString,
            String remoteUrl,
            String organizationId,
            String projectSlug,
            String signingPolicySlug,
            String artifactConfigurationSlug,
            String description) {

        wireMockRule.verify(postRequestedFor(urlEqualTo("/v1/" + organizationId + "/SigningRequests"))
                .withHeader("Authorization", equalTo("Bearer " + apiToken + ":" + trustedBuildSystemToken))
                .withRequestBodyPart(aMultipart().withBody(equalTo(projectSlug)).build())
                .withRequestBodyPart(aMultipart().withBody(equalTo(signingPolicySlug)).build())
                .withRequestBodyPart(aMultipart().withBody(equalTo(remoteUrl)).build())
                .withRequestBodyPart(aMultipart().withBody(equalTo(description)).build())
                .withRequestBodyPart(aMultipart().withBody(equalTo(artifactConfigurationSlug)).build()));

        assertFormFiles(unsignedArtifactString, organizationId);
    }

    private void assertFormFiles(String unsignedArtifactString, String organizationId) {
        Request r = wireMockRule.findAll(postRequestedFor(urlEqualTo("/v1/" + organizationId + "/SigningRequests"))).get(0);
        assertTrue(getMultipartFormDataFileContents(r, "Artifact").contains(unsignedArtifactString));
        String buildSettingsFile = getMultipartFormDataFileContents(r, "Origin.BuildData.BuildSettingsFile");
        assertTrue(buildSettingsFile.contains("node {writeFile text:"));
        assertTrue(buildSettingsFile.contains(organizationId));
    }

    private String getMultipartFormDataFileContents(Request r, String name) {
        
        for (Request.Part part : r.getParts())  
        { 
            if(name.equals(part.getName())) {
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