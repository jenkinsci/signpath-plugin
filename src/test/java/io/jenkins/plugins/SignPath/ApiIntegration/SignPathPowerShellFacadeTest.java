package io.jenkins.plugins.SignPath.ApiIntegration;

import io.jenkins.plugins.SignPath.ApiIntegration.Model.RepositoryMetadataModel;
import io.jenkins.plugins.SignPath.ApiIntegration.Model.SigningRequestModel;
import io.jenkins.plugins.SignPath.ApiIntegration.Model.SigningRequestOriginModel;
import io.jenkins.plugins.SignPath.ApiIntegration.PowerShell.PowerShellExecutionResult;
import io.jenkins.plugins.SignPath.ApiIntegration.PowerShell.PowerShellExecutor;
import io.jenkins.plugins.SignPath.ApiIntegration.PowerShell.SignPathPowerShellFacade;
import io.jenkins.plugins.SignPath.Common.TemporaryFile;
import io.jenkins.plugins.SignPath.Exceptions.SignPathFacadeCallException;
import io.jenkins.plugins.SignPath.TestUtils.Some;
import io.jenkins.plugins.SignPath.TestUtils.TemporaryFileUtil;
import org.junit.Before;
import org.junit.Rule;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.FromDataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.function.ThrowingRunnable;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

import static io.jenkins.plugins.SignPath.TestUtils.AssertionExtensions.assertContains;
import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(Theories.class)
public class SignPathPowerShellFacadeTest {
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private PowerShellExecutor powershellExecutor;

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
        when(powershellExecutor.execute(anyString())).then(a -> {
            capturedCommandArray[0] = a.getArgumentAt(0, String.class);
            return powerShellExecutionResult;
        });
    }

    @Theory
    public void submitSigningRequest(@FromDataPoints("allBooleans") boolean withOptionalFields) throws IOException, SignPathFacadeCallException {
        SigningRequestModel signingRequestModel = randomSigningRequest(withOptionalFields);

        // ACT
        TemporaryFile signedArtifactResultFile = sut.submitSigningRequest(signingRequestModel);

        // ASSERT
        assertNotNull(signedArtifactResultFile);
        String signedArtifactPath = TemporaryFileUtil.getAbsolutePathAndDispose(signedArtifactResultFile);
        String capturedCommand = getCapturedPowerShellCommand();
        assertNotNull(capturedCommand);

        assertContainsCredentials(credentials, capturedCommand);
        assertContainsConfiguration(apiConfiguration, capturedCommand, true);
        assertContainsSigningRequestModel(signingRequestModel, capturedCommand, withOptionalFields);
        assertContains(signedArtifactPath, capturedCommand);
    }

    @Theory
    public void submitSigningRequestAsync(@FromDataPoints("allBooleans") boolean withOptionalFields) throws IOException, SignPathFacadeCallException {
        SigningRequestModel signingRequestModel = randomSigningRequest(withOptionalFields);

        UUID organizationId = signingRequestModel.getOrganizationId();
        UUID signingRequestId = Some.uuid();

        powerShellExecutionResult = new PowerShellExecutionResult(false, "SHA256 hash: " + Some.sha1Hash() + "\n" +
                "Submitted signing request at 'https://app.signpath.io/api/v1/" + organizationId + "/SigningRequests/" + signingRequestId + "'\n" +
                signingRequestId);

        // ACT
        UUID result = sut.submitSigningRequestAsync(signingRequestModel);

        // ASSERT
        assertEquals(signingRequestId, result);
        String capturedCommand = getCapturedPowerShellCommand();
        assertNotNull(capturedCommand);

        assertContainsCredentials(credentials, capturedCommand);
        assertContainsConfiguration(apiConfiguration, capturedCommand, false);
        assertContainsSigningRequestModel(signingRequestModel, capturedCommand, withOptionalFields);
    }

    @Theory
    public void submitSigningRequestAsync_withUnexpectedOutput_throws() throws IOException {
        boolean withOptionalFields = Some.bool();
        SigningRequestModel signingRequestModel = randomSigningRequest(withOptionalFields);

        powerShellExecutionResult = new PowerShellExecutionResult(false, "some unexpected string");

        // ACT
        ThrowingRunnable act = () -> sut.submitSigningRequestAsync(signingRequestModel);

        // ASSERT
        Throwable ex = assertThrows(SignPathFacadeCallException.class, act);
        assertEquals("Unexpected output from PowerShell, did not find a valid signingRequestId.", ex.getMessage());
    }

    @Theory
    public void getSignedArtifact() throws IOException, SignPathFacadeCallException {
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

    @Theory
    public void submitSigningRequest_powerShellError_throws() {
        powerShellExecutionResult = new PowerShellExecutionResult(true, Some.stringNonEmpty());

        // ACT
        ThrowingRunnable act = () -> sut.submitSigningRequest(randomSigningRequest(Some.bool()));

        // ASSERT
        Throwable ex = assertThrows(SignPathFacadeCallException.class, act);
        assertEquals(String.format("PowerShell script exited with error: '%s'", powerShellExecutionResult.getOutput()), ex.getMessage());
    }

    @Theory
    public void submitSigningRequestAsync_powerShellError_throws() {
        powerShellExecutionResult = new PowerShellExecutionResult(true, Some.stringNonEmpty());

        // ACT
        ThrowingRunnable act = () -> sut.submitSigningRequestAsync(randomSigningRequest(Some.bool()));

        // ASSERT
        Throwable ex = assertThrows(SignPathFacadeCallException.class, act);
        assertEquals(String.format("PowerShell script exited with error: '%s'", powerShellExecutionResult.getOutput()), ex.getMessage());
    }

    @Theory
    public void getSignedArtifact_powerShellError_throws() {
        powerShellExecutionResult = new PowerShellExecutionResult(true, Some.stringNonEmpty());

        // ACT
        ThrowingRunnable act = () -> sut.getSignedArtifact(Some.uuid(), Some.uuid());

        // ASSERT
        Throwable ex = assertThrows(SignPathFacadeCallException.class, act);
        assertEquals(String.format("PowerShell script exited with error: '%s'", powerShellExecutionResult.getOutput()), ex.getMessage());
    }

    private String getCapturedPowerShellCommand() {
        return capturedCommandArray[0];
    }

    private SigningRequestModel randomSigningRequest(boolean withOptionalFields) throws IOException {
        UUID organizationId = Some.uuid();
        String projectSlug = Some.stringNonEmpty();
        String artifactConfigurationSlug = withOptionalFields ? Some.stringNonEmpty() : Some.stringEmptyOrNull();
        String signingPolicySlug = Some.stringNonEmpty();
        String description = withOptionalFields ? Some.stringNonEmpty() : Some.stringEmptyOrNull();
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
        return new SigningRequestModel(organizationId, projectSlug, artifactConfigurationSlug, signingPolicySlug, description, origin, unsignedArtifact);
    }

    private void assertContainsSigningRequestModel(SigningRequestModel signingRequestModel, String capturedCommand, boolean withOptionalFields) {
        SigningRequestOriginModel origin = signingRequestModel.getOrigin();
        RepositoryMetadataModel repositoryMetadata = origin.getRepositoryMetadata();
        assertContains(signingRequestModel.getOrganizationId().toString(), capturedCommand);
        assertContains(signingRequestModel.getProjectSlug(), capturedCommand);
        assertContains(signingRequestModel.getSigningPolicySlug(), capturedCommand);

        String artifactConfigurationSlug = signingRequestModel.getArtifactConfigurationSlug();
        String description = signingRequestModel.getDescription();
        if (withOptionalFields) {
            assertTrue(artifactConfigurationSlug != null && !artifactConfigurationSlug.isEmpty());
            assertTrue(description != null && !description.isEmpty());

            assertContains(artifactConfigurationSlug, capturedCommand);
            assertContains(description, capturedCommand);
        } else {
            assertTrue(artifactConfigurationSlug == null || artifactConfigurationSlug.isEmpty());
            assertTrue(description == null || description.isEmpty());
        }

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

    private void assertContainsConfiguration(ApiConfiguration apiConfiguration, String capturedCommand, boolean withWaitTime) {
        assertContains(apiConfiguration.getApiUrl().toString(), capturedCommand);
        assertContains(String.valueOf(apiConfiguration.getServiceUnavailableTimeoutInSeconds()), capturedCommand);
        assertContains(String.valueOf(apiConfiguration.getUploadAndDownloadRequestTimeoutInSeconds()), capturedCommand);

        if (withWaitTime) {
            assertContains(String.valueOf(apiConfiguration.getWaitForCompletionTimeoutInSeconds()), capturedCommand);
        }
    }

    @DataPoints("allBooleans")
    public static boolean[] allBooleans() {
        return new boolean[]{true, false};
    }
}
