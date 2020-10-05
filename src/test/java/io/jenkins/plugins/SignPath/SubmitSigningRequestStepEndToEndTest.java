package io.jenkins.plugins.SignPath;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import hudson.Launcher;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.model.queue.QueueTaskFuture;
import hudson.plugins.git.util.BuildData;
import io.jenkins.plugins.SignPath.Artifacts.ArtifactFileManager;
import io.jenkins.plugins.SignPath.Common.TemporaryFile;
import io.jenkins.plugins.SignPath.Exceptions.SignPathFacadeCallException;
import io.jenkins.plugins.SignPath.TestUtils.*;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
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

@RunWith(Theories.class)
public class SubmitSigningRequestStepEndToEndTest {
    private static final int MockServerPort = 51000;

    @Rule
    public SignPathJenkinsRule j= new SignPathJenkinsRule();

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(MockServerPort);

    @Theory
    public void submitSigningRequest(@FromDataPoints("allBooleans") Boolean withOptionalFields) throws Exception  {
        Launcher launcher = j.createLocalLauncher();
        TaskListener listener = j.createTaskListener();
        Jenkins jenkins = j.jenkins;
        byte[] signedArtifactBytes = Some.bytes();
        CredentialsStore credentialStore = CredentialStoreUtils.getCredentialStore(jenkins);
        assert credentialStore != null;

        String unsignedArtifactString = Some.stringNonEmpty();

        String trustedBuildSystemToken = Some.stringNonEmpty();
        String ciUserToken = Some.stringNonEmpty();

        String projectSlug = Some.stringNonEmpty();
        String signingPolicySlug = Some.stringNonEmpty();
        String organizationId = Some.uuid().toString();

        String artifactConfigurationSlug = Some.stringNonEmpty();
        String description = Some.stringNonEmpty();

        CredentialStoreUtils.addCredentials(credentialStore, CredentialsScope.SYSTEM, Constants.TrustedBuildSystemTokenCredentialId, trustedBuildSystemToken);

        String apiUrl = getMockUrl();
        String getSigningRequestStatus = "getSigningRequestStatus";
        String downloadSignedArtifact = "downloadSignedArtifact";
        wireMockRule.stubFor(post(urlEqualTo("/v1/" + organizationId + "/SigningRequests"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Location", getMockUrl(getSigningRequestStatus))));
        wireMockRule.stubFor(get(urlEqualTo("/" + getSigningRequestStatus))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{status: 'Completed', signedArtifactLink: '" + getMockUrl(downloadSignedArtifact) + "'}")));

        wireMockRule.stubFor(get(urlEqualTo("/" + downloadSignedArtifact))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(signedArtifactBytes)));

        String optionalFields ="";
        if(withOptionalFields){
            optionalFields = "artifactConfigurationSlug: '"+artifactConfigurationSlug+"'," +
                    "description: '"+description+"',";
        }

        WorkflowJob workflowJob = j.createWorkflow("SignPath",
                "writeFile text: '"+unsignedArtifactString+"', file: 'unsigned.exe'; " +
                        "archiveArtifacts artifacts: 'unsigned.exe', fingerprint: true; " +
                        "submitSigningRequest( apiUrl: '" + apiUrl + "', " +
                        "inputArtifactPath: 'unsigned.exe', " +
                        "outputArtifactPath: 'signed.exe', " +
                        "ciUserToken: '" + ciUserToken + "'," +
                        "organizationId: '" + organizationId + "'," +
                        "projectSlug: '" + projectSlug + "'," +
                        "signingPolicySlug: '" + signingPolicySlug + "'," +
                        optionalFields+
                        "waitForCompletion: 'true'," +
                        "serviceUnavailableTimeoutInSeconds: 10," +
                        "uploadAndDownloadRequestTimeoutInSeconds: 10," +
                        "waitForCompletionTimeoutInSeconds: 10);");

        String remoteUrl = Some.url();
        BuildData buildData = new BuildData(Some.stringNonEmpty());
        buildData.saveBuild(BuildDataDomainObjectMother.CreateRandomBuild(1));
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

        ArtifactFileManager artifactFileManager = new ArtifactFileManager(run, launcher, listener);
        TemporaryFile signedArtifact = artifactFileManager.retrieveArtifact("signed.exe");
        byte[] signedArtifactContent = TemporaryFileUtil.getContentAndDispose(signedArtifact);
        assertArrayEquals(signedArtifactBytes, signedArtifactContent);

        wireMockRule.verify(postRequestedFor(urlEqualTo("/v1/" + organizationId + "/SigningRequests"))
                .withHeader("Authorization", equalTo("Bearer " + ciUserToken + ":" + trustedBuildSystemToken))
                .withRequestBodyPart(aMultipart().withBody(equalTo(projectSlug)).build())
                .withRequestBodyPart(aMultipart().withBody(equalTo(signingPolicySlug)).build())
                .withRequestBodyPart(aMultipart().withBody(equalTo(remoteUrl)).build()));

        if(withOptionalFields)
        {
            wireMockRule.verify(postRequestedFor(urlEqualTo("/v1/" + organizationId + "/SigningRequests"))
                    .withRequestBodyPart(aMultipart().withBody(equalTo(description)).build())
                    .withRequestBodyPart(aMultipart().withBody(equalTo(artifactConfigurationSlug)).build()));
        }

        Request r=wireMockRule.findAll(postRequestedFor(urlEqualTo("/v1/" + organizationId + "/SigningRequests"))).get(0);

        assertEquals(unsignedArtifactString, getMultipartFormDataFileContents(r, "Artifact"));

        String buildSettingsFile = getMultipartFormDataFileContents(r, "Origin.BuildSettingsFile");
        assertTrue(buildSettingsFile.contains("node {writeFile text:"));
        assertTrue(buildSettingsFile.contains(organizationId));
    }

    @Theory
    public void submitSigningRequestWithMissingField() throws Exception  {
        WorkflowJob workflowJob = j.createWorkflow("SignPath","submitSigningRequest();");

        BuildData buildData = new BuildData(Some.stringNonEmpty());
        buildData.saveBuild(BuildDataDomainObjectMother.CreateRandomBuild(1));
        buildData.addRemoteUrl(Some.url());

        // ACT
        QueueTaskFuture<WorkflowRun> runFuture = workflowJob.scheduleBuild2(0, buildData);
        assert runFuture != null;
        WorkflowRun run = runFuture.get();

        // ASSERT
        assertEquals(Result.FAILURE, run.getResult());
        assertTrue(run.getLog().contains("SignPathStepInvalidArgumentException"));
    }

    private String getMultipartFormDataFileContents(Request r, String name) throws IOException {
        String bodyAsString = r.getBodyAsString();
        Matcher regexResult = Pattern.compile(String.format("name=%s; filename=.*?\\n(.*?)--", name), Pattern.DOTALL).matcher(bodyAsString);
        if(!regexResult.find()){
            fail("multipart-form-data with name "+name+" not found in "+ bodyAsString);
        }

        return regexResult.group(1).trim();
    }

    private String getMockUrl(){
        return getMockUrl("");
    }

    private String getMockUrl(String postfix){
        return String.format("http://localhost:%d/%s", MockServerPort, postfix);
    }

    @DataPoints("allBooleans")
    public static Boolean[] allBooleans(){
        return new Boolean[]{true, false};
    }
}
