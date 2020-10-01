package io.jenkins.plugins.SignPath.ApiIntegration;

import io.jenkins.plugins.SignPath.ApiIntegration.Model.RepositoryMetadataModel;
import io.jenkins.plugins.SignPath.ApiIntegration.Model.SigningRequestModel;
import io.jenkins.plugins.SignPath.ApiIntegration.Model.SigningRequestOriginModel;
import io.jenkins.plugins.SignPath.ApiIntegration.PowerShell.IPowerShellExecutor;
import io.jenkins.plugins.SignPath.ApiIntegration.PowerShell.PowerShellExecutionResult;
import io.jenkins.plugins.SignPath.ApiIntegration.PowerShell.SignPathPowerShellFacade;
import io.jenkins.plugins.SignPath.Common.TemporaryFile;
import io.jenkins.plugins.SignPath.TestUtils.Some;
import io.jenkins.plugins.SignPath.TestUtils.TemporaryFileUtil;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

import static io.jenkins.plugins.SignPath.TestUtils.AssertionExtensions.assertContains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

public class SignPathPowerShellFacadeTest {
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private IPowerShellExecutor powershellExecutor;

    private SignPathCredentials credentials;
    private ApiConfiguration apiConfiguration;
    private SignPathPowerShellFacade sut;
    private final String[] capturedCommandArray = new String[1];
    private PowerShellExecutionResult powerShellExecutionResult;

    @Before
    public void setup() throws MalformedURLException {
        credentials = new SignPathCredentials(Some.stringNonEmpty(), Some.stringNonEmpty());
        apiConfiguration = new ApiConfiguration(new URL(Some.url()), Some.integer(), Some.integer(), Some.integer());
        sut = new SignPathPowerShellFacade(powershellExecutor, credentials, apiConfiguration);

        powerShellExecutionResult = new PowerShellExecutionResult(false, Some.stringNonEmpty());
        when(powershellExecutor.execute(anyString())).then(a-> {
            capturedCommandArray[0] = a.getArgumentAt(0, String.class);
            return powerShellExecutionResult;
        });
    }

    @Test
    public void submitSigningRequest() throws IOException {
        SigningRequestModel signingRequestModel = randomSigningRequest();

        // ACT
        TemporaryFile signedArtifactResultFile = sut.submitSigningRequest(signingRequestModel);

        // ASSERT
        assertNotNull(signedArtifactResultFile);
        String signedArtifactPath = TemporaryFileUtil.getAbsolutePathAndDispose(signedArtifactResultFile);
        String capturedCommand = getCapturedPowerShellCommand();
        assertNotNull(capturedCommand);

        assertContainsCredentials(credentials, capturedCommand);
        assertContainsConfiguration(apiConfiguration, capturedCommand, true);
        assertContainsSigningRequestModel(signingRequestModel, capturedCommand);
        assertContains(signedArtifactPath, capturedCommand);
    }

    @Test
    public void submitSigningRequestAsync() throws IOException {
        SigningRequestModel signingRequestModel = randomSigningRequest();

        UUID organizationId = signingRequestModel.getOrganizationId();
        UUID signingRequestId = Some.uuid();

        powerShellExecutionResult = new PowerShellExecutionResult(false,"SHA256 hash: "+Some.sha1Hash()+"\n" +
                "Submitted signing request at 'https://app.signpath.io/api/v1/"+organizationId+"/SigningRequests/"+signingRequestId+"'\n" +
                signingRequestId);

        // ACT
        UUID result = sut.submitSigningRequestAsync(signingRequestModel);

        // ASSERT
        assertEquals(signingRequestId, result);
        String capturedCommand = getCapturedPowerShellCommand();
        assertNotNull(capturedCommand);

        assertContainsCredentials(credentials, capturedCommand);
        assertContainsConfiguration(apiConfiguration, capturedCommand, false);
        assertContainsSigningRequestModel(signingRequestModel, capturedCommand);
    }

    @Test
    public void getSignedArtifact() throws IOException {
        UUID organizationId = Some.uuid();
        UUID signingRequestId = Some.uuid();

        // ACT
        TemporaryFile signedArtifactResultFile = sut.getSignedArtifact(organizationId, signingRequestId);

        // ASSERT
        assertNotNull(signedArtifactResultFile);
        String signedArtifactPath = TemporaryFileUtil.getAbsolutePathAndDispose(signedArtifactResultFile);
        String capturedCommand = getCapturedPowerShellCommand();
        assertNotNull(capturedCommand);

        assertContainsCredentials(credentials, capturedCommand);
        assertContainsConfiguration(apiConfiguration, capturedCommand, true);
        assertContains(organizationId.toString(), capturedCommand);
        assertContains(signingRequestId.toString(), capturedCommand);
        assertContains(signedArtifactPath, capturedCommand);
    }

    private String getCapturedPowerShellCommand(){
        return capturedCommandArray[0];
    }

    private SigningRequestModel randomSigningRequest() throws IOException {
        UUID organizationId = Some.uuid();
        String projectSlug = Some.stringNonEmpty();
        String artifactConfigurationSlug = Some.stringNonEmpty();
        String signingPolicySlug = Some.stringNonEmpty();
        String description = Some.stringNonEmpty();
        String sourceControlManagementType = Some.stringNonEmpty();
        String repositoryUrl = Some.stringNonEmpty();
        String branchName = Some.stringNonEmpty();
        String commidId = Some.sha1Hash();
        String buildUrl = Some.url();
        byte[] buildSettingsArtifactBytes = Some.bytes();
        byte[] unsignedArtifactBytes = Some.bytes();
        TemporaryFile buildSettingsFile = TemporaryFileUtil.create(buildSettingsArtifactBytes);
        TemporaryFile unsignedArtifact = TemporaryFileUtil.create(unsignedArtifactBytes);

        RepositoryMetadataModel repositoryMetadata = new RepositoryMetadataModel(sourceControlManagementType, repositoryUrl, branchName, commidId);
        SigningRequestOriginModel origin = new SigningRequestOriginModel(repositoryMetadata, buildUrl, buildSettingsFile);
        SigningRequestModel signingRequestModel = new SigningRequestModel(organizationId, projectSlug, artifactConfigurationSlug, signingPolicySlug, description, origin,unsignedArtifact);
        return signingRequestModel;
    }

    private void assertContainsSigningRequestModel(SigningRequestModel signingRequestModel, String capturedCommand) {
        SigningRequestOriginModel origin = signingRequestModel.getOrigin();
        RepositoryMetadataModel repositoryMetadata = origin.getRepositoryMetadata();
        assertContains(signingRequestModel.getOrganizationId().toString(), capturedCommand);
        assertContains(signingRequestModel.getProjectSlug(), capturedCommand);
        assertContains(signingRequestModel.getArtifactConfigurationSlug(), capturedCommand);
        assertContains(signingRequestModel.getSigningPolicySlug(), capturedCommand);
        assertContains(signingRequestModel.getDescription(), capturedCommand);
        assertContains(repositoryMetadata.getSourceControlManagementType(), capturedCommand);
        assertContains(repositoryMetadata.getRepositoryUrl(), capturedCommand);
        assertContains(repositoryMetadata.getBranchName(), capturedCommand);
        assertContains(repositoryMetadata.getCommitId(), capturedCommand);
        assertContains(origin.getBuildUrl(), capturedCommand);
        assertContains(origin.getBuildSettingsFile().getAbsolutePath(), capturedCommand);
        assertContains(signingRequestModel.getArtifact().getAbsolutePath(), capturedCommand);
    }

    private void assertContainsCredentials(SignPathCredentials credentials, String capturedCommand) {
        assertContains(credentials.getCiUserToken(), capturedCommand);
        assertContains(credentials.getTrustedBuildSystemToken(), capturedCommand);
        assertContains(credentials.toString(), capturedCommand);
    }

    private void assertContainsConfiguration(ApiConfiguration apiConfiguration, String capturedCommand, Boolean withWaitTime) {
        assertContains(apiConfiguration.getApiUrl().toString(), capturedCommand);
        assertContains(String.valueOf(apiConfiguration.getServiceUnavailableTimeoutInSeconds()), capturedCommand);
        assertContains(String.valueOf(apiConfiguration.getUploadAndDownloadRequestTimeoutInSeconds()), capturedCommand);

        if(withWaitTime) {
            assertContains(String.valueOf(apiConfiguration.getWaitForCompletionTimeoutInSeconds()), capturedCommand);
        }
    }
}
