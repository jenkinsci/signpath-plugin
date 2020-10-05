package io.jenkins.plugins.SignPath;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import hudson.Launcher;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.plugins.git.Branch;
import hudson.plugins.git.Revision;
import hudson.plugins.git.util.Build;
import hudson.plugins.git.util.BuildData;
import hudson.util.Secret;
import io.jenkins.plugins.SignPath.Artifacts.ArtifactFileManager;
import io.jenkins.plugins.SignPath.Common.TemporaryFile;
import io.jenkins.plugins.SignPath.TestUtils.SignPathJenkinsRule;
import io.jenkins.plugins.SignPath.TestUtils.Some;
import io.jenkins.plugins.SignPath.TestUtils.TemporaryFileUtil;
import jenkins.model.Jenkins;
import org.eclipse.jgit.lib.ObjectId;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.*;

public class SubmitSigningRequestStepEndToEndTest {
    @Rule
    public SignPathJenkinsRule j= new SignPathJenkinsRule();

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(51000);

    @Test
    public void submitSigningRequest() throws Exception {
        Launcher launcher = j.createLocalLauncher();
        TaskListener listener = j.createTaskListener();
        Jenkins jenkins = j.jenkins;
        byte[] signedArtifactBytes = Some.bytes();
        CredentialsStore credentialStore = getCredentialStore(jenkins);
        String trustedBuildSystemToken = Some.stringNonEmpty();
        String ciUserToken = Some.stringNonEmpty();

        addCredentials(credentialStore, CredentialsScope.SYSTEM, Constants.TrustedBuildSystemTokenCredentialId, trustedBuildSystemToken);

        String organizationId = Some.uuid().toString();
        wireMockRule.stubFor(post(urlEqualTo("/v1/"+organizationId+"/SigningRequests"))
               .willReturn(aResponse()
                               .withStatus(200)
                               .withHeader("Location", "http://localhost:51000/getSigningRequestStatus")));
        wireMockRule.stubFor(get(urlEqualTo("/getSigningRequestStatus"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{status: 'Completed', signedArtifactLink: 'http://localhost:51000/downloadSignedArtifact'}")));

        wireMockRule.stubFor(get(urlEqualTo("/downloadSignedArtifact"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(signedArtifactBytes)));

        WorkflowJob workflowJob = j.createWorkflow("SignPath",
                "writeFile text: 'hello', file: 'unsigned.exe'; " +
                        "archiveArtifacts artifacts: 'unsigned.exe', fingerprint: true; "+
                        "submitSigningRequest( apiUrl: 'http://localhost:51000/', " +
                        "inputArtifactPath: 'unsigned.exe', "+
                        "outputArtifactPath: 'signed.exe', " +
                        "ciUserToken: '"+ciUserToken+"',"+
                        "organizationId: '"+organizationId+"'," +
                        "projectSlug: 'Teamcity'," +
                        "signingPolicySlug: 'TestSigning'," +
                        "waitForCompletion: 'true'," +
                        "serviceUnavailableTimeoutInSeconds: 10,"+
                        "uploadAndDownloadRequestTimeoutInSeconds: 10,"+
                        "waitForCompletionTimeoutInSeconds: 10);");

        BuildData buildData = new BuildData(Some.stringNonEmpty());
        buildData.saveBuild(CreateRandomBuild(1));
        buildData.addRemoteUrl(Some.url());

        // ACT
        WorkflowRun run = workflowJob.scheduleBuild2(0, buildData).get();
        if(run.getResult() != Result.SUCCESS){
            assertEquals("", run.getLog() + run.getResult());
            fail();
        }

        wireMockRule.verify(postRequestedFor(urlEqualTo("/v1/"+organizationId+"/SigningRequests"))
                .withHeader("Authorization", equalTo("Bearer"+ciUserToken+":"+trustedBuildSystemToken)));

        ArtifactFileManager artifactFileManager = new ArtifactFileManager(run,launcher, listener);

        // ASSERT
        TemporaryFile signedArtifact = artifactFileManager.retrieveArtifact("signed.exe");
        byte[] signedArtifactContent = TemporaryFileUtil.getContentAndDispose(signedArtifact);
        assertArrayEquals(signedArtifactBytes, signedArtifactContent);
    }

    private Build CreateRandomBuild(int buildNumber) {
        String commitId = Some.sha1Hash();
        int branchCount = Some.integer(1, 2);
        Branch[] branches = new Branch[branchCount];
        for (int i = 0; i < branchCount; i++) {
            branches[i] = CreateRandomBranch();
        }

        return CreateBuild(buildNumber, commitId, branches);
    }

    private Build CreateBuild(int buildNumber, String commitId, Branch... branches){
        Result buildResult = Result.SUCCESS;
        Revision revision = new Revision(ObjectId.fromString(commitId), Arrays.asList(branches));
        return new Build(revision, buildNumber, buildResult);
    }

    private Branch CreateRandomBranch(){
        return new Branch(Some.stringNonEmpty(), ObjectId.fromString(Some.sha1Hash()));
    }

    private Branch CreateBranch(String branchId, String branchName){
        return new Branch(branchName, ObjectId.fromString(branchId));
    }

    private void addCredentials(CredentialsStore credentialsStore, CredentialsScope scope, String id, String secret) throws IOException {
        Domain domain = credentialsStore.getDomains().get(0);
        credentialsStore.addCredentials(domain,
                new StringCredentialsImpl(scope, id, Some.stringNonEmpty(), Secret.fromString(secret)));
    }

    private CredentialsStore getCredentialStore(Jenkins jenkins) {
        for (CredentialsStore credentialsStore : CredentialsProvider.lookupStores(jenkins)) {
            if(SystemCredentialsProvider.StoreImpl.class.isAssignableFrom(credentialsStore.getClass())){
                return credentialsStore;
            }
        }

        return null;
    }
}
