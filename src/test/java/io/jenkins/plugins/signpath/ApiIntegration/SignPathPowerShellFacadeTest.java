package io.jenkins.plugins.signpath.ApiIntegration;

import io.jenkins.plugins.signpath.ApiIntegration.Model.RepositoryMetadataModel;
import io.jenkins.plugins.signpath.ApiIntegration.Model.SigningRequestModel;
import io.jenkins.plugins.signpath.ApiIntegration.Model.SigningRequestOriginModel;
import io.jenkins.plugins.signpath.ApiIntegration.Model.SubmitSigningRequestResult;
import io.jenkins.plugins.signpath.ApiIntegration.PowerShell.PowerShellCommand;
import io.jenkins.plugins.signpath.ApiIntegration.PowerShell.PowerShellExecutionResult;
import io.jenkins.plugins.signpath.ApiIntegration.PowerShell.PowerShellExecutor;
import io.jenkins.plugins.signpath.ApiIntegration.PowerShell.SignPathPowerShellFacade;
import io.jenkins.plugins.signpath.Common.TemporaryFile;
import io.jenkins.plugins.signpath.Exceptions.SignPathFacadeCallException;
import io.jenkins.plugins.signpath.Exceptions.SignPathStepInvalidArgumentException;
import io.jenkins.plugins.signpath.TestUtils.Some;
import io.jenkins.plugins.signpath.TestUtils.TemporaryFileUtil;
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
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.util.UUID;

import static io.jenkins.plugins.signpath.TestUtils.AssertionExtensions.assertContains;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;

@RunWith(Theories.class)
public class SignPathPowerShellFacadeTest {
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private PowerShellExecutor powershellExecutor;

    private SignPathCredentials credentials;
    private ApiConfiguration apiConfiguration;

    @Mock
    private PrintStream logger;

    private SignPathPowerShellFacade sut;
    private PowerShellCommand capturedCommand;
    private PowerShellExecutionResult powerShellExecutionResult;

    @Before
    public void setup() throws MalformedURLException, SignPathStepInvalidArgumentException {
        credentials = new SignPathCredentials(Some.stringNonEmpty(), Some.stringNonEmpty());
        apiConfiguration = Some.apiConfiguration();
        sut = new SignPathPowerShellFacade(powershellExecutor, credentials, apiConfiguration, logger);

        powerShellExecutionResult = PowerShellExecutionResult.Success(Some.stringNonEmpty());
        when(powershellExecutor.execute(any(PowerShellCommand.class), anyInt())).then(a -> {
            capturedCommand = a.getArgumentAt(0, PowerShellCommand.class);
            return powerShellExecutionResult;
        });
    }

    @Theory
    public void submitSigningRequest(@FromDataPoints("allBooleans") boolean withOptionalFields) throws IOException, SignPathFacadeCallException {
        SigningRequestModel signingRequestModel = randomSigningRequest(withOptionalFields);
        UUID organizationId = signingRequestModel.getOrganizationId();
        UUID signingRequestId = Some.uuid();

        String separator = System.getProperty("line.separator");
        powerShellExecutionResult = PowerShellExecutionResult.Success("SHA256 hash: " + Some.sha1Hash() + separator +
                "Submitted signing request at 'https://app.signpath.io/api/v1/" + organizationId + "/SigningRequests/" + signingRequestId + "'" + separator +
                signingRequestId);

        // ACT
        SubmitSigningRequestResult result = sut.submitSigningRequest(signingRequestModel);

        // ASSERT
        assertNotNull(result);
        assertEquals(signingRequestId, result.getSigningRequestId());
        String signedArtifactPath = TemporaryFileUtil.getAbsolutePathAndDispose(result.getSignedArtifact());
        assertNotNull(capturedCommand);

        assertContainsCredentials(credentials, capturedCommand);
        assertContainsConfiguration(apiConfiguration, capturedCommand, true);
        assertContainsSigningRequestModel(signingRequestModel, capturedCommand, withOptionalFields);
        assertContainsParameter("OutputArtifactPath", signedArtifactPath, capturedCommand);
        assertContainsFlag("WaitForCompletion", capturedCommand);
        assertContainsFlag("Force", capturedCommand);
    }

    @Theory
    public void submitSigningRequestAsync(@FromDataPoints("allBooleans") boolean withOptionalFields) throws IOException, SignPathFacadeCallException {
        SigningRequestModel signingRequestModel = randomSigningRequest(withOptionalFields);

        UUID organizationId = signingRequestModel.getOrganizationId();
        UUID signingRequestId = Some.uuid();

        String separator = System.getProperty("line.separator");
        powerShellExecutionResult = PowerShellExecutionResult.Success("SHA256 hash: " + Some.sha1Hash() + separator +
                "Submitted signing request at 'https://app.signpath.io/api/v1/" + organizationId + "/SigningRequests/" + signingRequestId + "'" + separator +
                signingRequestId);

        // ACT
        UUID result = sut.submitSigningRequestAsync(signingRequestModel);

        // ASSERT
        assertEquals(signingRequestId, result);
        assertNotNull(capturedCommand);

        assertContainsCredentials(credentials, capturedCommand);
        assertContainsConfiguration(apiConfiguration, capturedCommand, false);
        assertContainsSigningRequestModel(signingRequestModel, capturedCommand, withOptionalFields);
    }

    @Theory
    public void submitSigningRequestAsync_withUnexpectedOutput_throws() throws IOException {
        boolean withOptionalFields = Some.bool();
        SigningRequestModel signingRequestModel = randomSigningRequest(withOptionalFields);

        powerShellExecutionResult = PowerShellExecutionResult.Success("some unexpected string");

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
        assertNotNull(capturedCommand);

        assertContainsCredentials(credentials, capturedCommand);
        assertContainsConfiguration(apiConfiguration, capturedCommand, true);
        assertContainsParameter("OrganizationId", organizationId.toString(), capturedCommand);
        assertContainsParameter("SigningRequestId", signingRequestId.toString(), capturedCommand);
        assertContainsParameter("OutputArtifactPath", signedArtifactPath, capturedCommand);
        assertContainsFlag("Force", capturedCommand);
    }

    @Theory
    public void submitSigningRequest_powerShellError_throws() {
        powerShellExecutionResult = PowerShellExecutionResult.Error(Some.stringNonEmpty());

        // ACT
        ThrowingRunnable act = () -> sut.submitSigningRequest(randomSigningRequest(Some.bool()));

        // ASSERT
        Throwable ex = assertThrows(SignPathFacadeCallException.class, act);
        assertEquals(String.format("PowerShell script exited with error: '%s'", powerShellExecutionResult.getErrorDescription()), ex.getMessage());
    }

    @Theory
    public void submitSigningRequestAsync_powerShellError_throws() {
        powerShellExecutionResult = PowerShellExecutionResult.Error(Some.stringNonEmpty());

        // ACT
        ThrowingRunnable act = () -> sut.submitSigningRequestAsync(randomSigningRequest(Some.bool()));

        // ASSERT
        Throwable ex = assertThrows(SignPathFacadeCallException.class, act);
        assertEquals(String.format("PowerShell script exited with error: '%s'", powerShellExecutionResult.getErrorDescription()), ex.getMessage());
    }

    @Theory
    public void getSignedArtifact_powerShellError_throws() {
        powerShellExecutionResult = PowerShellExecutionResult.Error(Some.stringNonEmpty());

        // ACT
        ThrowingRunnable act = () -> sut.getSignedArtifact(Some.uuid(), Some.uuid());

        // ASSERT
        Throwable ex = assertThrows(SignPathFacadeCallException.class, act);
        assertEquals(String.format("PowerShell script exited with error: '%s'", powerShellExecutionResult.getErrorDescription()), ex.getMessage());
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
        String commitId = Some.sha1Hash();
        String buildUrl = Some.url();
        byte[] buildSettingsArtifactBytes = Some.bytes();
        byte[] unsignedArtifactBytes = Some.bytes();
        TemporaryFile buildSettingsFile = TemporaryFileUtil.create(buildSettingsArtifactBytes);
        TemporaryFile unsignedArtifact = TemporaryFileUtil.create(unsignedArtifactBytes);

        RepositoryMetadataModel repositoryMetadata = new RepositoryMetadataModel(sourceControlManagementType, repositoryUrl, branchName, commitId);
        SigningRequestOriginModel origin = new SigningRequestOriginModel(repositoryMetadata, buildUrl, buildSettingsFile);
        return new SigningRequestModel(organizationId, projectSlug, artifactConfigurationSlug, signingPolicySlug, description, origin, unsignedArtifact);
    }

    private void assertContainsSigningRequestModel(SigningRequestModel signingRequestModel, PowerShellCommand capturedCommand, boolean withOptionalFields) {
        SigningRequestOriginModel origin = signingRequestModel.getOrigin();
        RepositoryMetadataModel repositoryMetadata = origin.getRepositoryMetadata();
        assertContainsParameter("OrganizationId", signingRequestModel.getOrganizationId().toString(), capturedCommand);
        assertContainsParameter("ProjectSlug", signingRequestModel.getProjectSlug(), capturedCommand);
        assertContainsParameter("SigningPolicySlug", signingRequestModel.getSigningPolicySlug(), capturedCommand);

        String artifactConfigurationSlug = signingRequestModel.getArtifactConfigurationSlug();
        String description = signingRequestModel.getDescription();
        if (withOptionalFields) {
            assertTrue(artifactConfigurationSlug != null && !artifactConfigurationSlug.isEmpty());
            assertTrue(description != null && !description.isEmpty());

            assertContainsParameter("ArtifactConfigurationSlug", artifactConfigurationSlug, capturedCommand);
            assertContainsParameter("Description", description, capturedCommand);
        } else {
            assertTrue(artifactConfigurationSlug == null || artifactConfigurationSlug.isEmpty());
            assertTrue(description == null || description.isEmpty());
        }

        assertContainsParameter("SourceControlManagementType", repositoryMetadata.getSourceControlManagementType(), capturedCommand);
        assertContainsParameter("RepositoryUrl", repositoryMetadata.getRepositoryUrl(), capturedCommand);
        assertContainsParameter("BranchName", repositoryMetadata.getBranchName(), capturedCommand);
        assertContainsParameter("CommitId", repositoryMetadata.getCommitId(), capturedCommand);
        assertContainsParameter("BuildUrl", origin.getBuildUrl(), capturedCommand);
        assertContainsParameter("BuildSettingsFile", "@" + origin.getBuildSettingsFile().getAbsolutePath(), capturedCommand);

        assertContainsParameter("InputArtifactPath", signingRequestModel.getArtifact().getAbsolutePath(), capturedCommand);
    }

    private void assertContainsCredentials(SignPathCredentials credentials, PowerShellCommand capturedCommand) {
        assertContainsParameter("CIUserToken", credentials.toCredentialString(), capturedCommand);
    }

    private void assertContainsConfiguration(ApiConfiguration apiConfiguration, PowerShellCommand capturedCommand, boolean withWaitTime) {
        assertContainsParameter("ApiUrl", apiConfiguration.getApiUrl().toString(), capturedCommand);
        assertContainsParameter("ServiceUnavailableTimeoutInSeconds", String.valueOf(apiConfiguration.getServiceUnavailableTimeoutInSeconds()), capturedCommand);
        assertContainsParameter("UploadAndDownloadRequestTimeoutInSeconds", String.valueOf(apiConfiguration.getUploadAndDownloadRequestTimeoutInSeconds()), capturedCommand);

        if (withWaitTime) {
            assertContainsParameter("WaitForCompletionTimeoutInSeconds", String.valueOf(apiConfiguration.getWaitForCompletionTimeoutInSeconds()), capturedCommand);
        }
    }

    private void assertContainsParameter(String name, String value, PowerShellCommand command) {
        assertContains(name, command.getCommand());

        assertTrue(command.getEnvironmentVariables().containsKey(name));
        assertEquals(command.getEnvironmentVariables().get(name), value);
    }

    private void assertContainsFlag(String flag, PowerShellCommand command) {
        assertContains(String.format("-%s", flag), command.getCommand());
    }

    @DataPoints("allBooleans")
    public static boolean[] allBooleans() {
        return new boolean[]{true, false};
    }
}
