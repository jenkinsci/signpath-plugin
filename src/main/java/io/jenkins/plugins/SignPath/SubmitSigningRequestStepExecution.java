package io.jenkins.plugins.SignPath;

import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import io.jenkins.plugins.SignPath.ApiIntegration.ApiConfiguration;
import io.jenkins.plugins.SignPath.ApiIntegration.ISignPathFacade;
import io.jenkins.plugins.SignPath.ApiIntegration.ISignPathFacadeFactory;
import io.jenkins.plugins.SignPath.ApiIntegration.Model.SigningRequestModel;
import io.jenkins.plugins.SignPath.ApiIntegration.Model.SigningRequestOriginModel;
import io.jenkins.plugins.SignPath.ApiIntegration.PowerShell.PowerShellExecutor;
import io.jenkins.plugins.SignPath.ApiIntegration.PowerShell.SignPathPowerShellFacadeFactory;
import io.jenkins.plugins.SignPath.ApiIntegration.SignPathCredentials;
import io.jenkins.plugins.SignPath.Artifacts.ArtifactFileManager;
import io.jenkins.plugins.SignPath.Common.TemporaryFile;
import io.jenkins.plugins.SignPath.Exceptions.OriginNotRetrievableException;
import io.jenkins.plugins.SignPath.Exceptions.SecretNotFoundException;
import io.jenkins.plugins.SignPath.Exceptions.SignPathFacadeCallException;
import io.jenkins.plugins.SignPath.OriginRetrieval.DefaultConfigFileProvider;
import io.jenkins.plugins.SignPath.OriginRetrieval.OriginRetriever;
import io.jenkins.plugins.SignPath.SecretRetrieval.CredentialBasedSecretRetriever;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousStepExecution;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.UUID;

public class SubmitSigningRequestStepExecution extends SynchronousStepExecution<String> {
    private final SubmitSigningRequestStep signStep;

    protected SubmitSigningRequestStepExecution(SubmitSigningRequestStep signStep, StepContext context) {
        super(context);
        this.signStep = signStep;
    }

    @Override
    protected String run() throws IOException, InterruptedException {
        TaskListener listener = getContext().get(TaskListener.class);
        assert listener != null;
        Run<?, ?> run = getContext().get(Run.class);
        Launcher launcher = getContext().get(Launcher.class);
        PrintStream logger = listener.getLogger();
        Jenkins jenkins = Jenkins.get();
        String jenkinsRootUrl = jenkins.getConfiguredRootUrl();

        logger.printf("signArtifact organizationId:%s waitForCompletion: %s", signStep.getOrganizationId(), signStep.getWaitForCompletion());

        // TODO SIGN-3326: Add to dependency-injection
        CredentialBasedSecretRetriever credentialSecretRetriever = new CredentialBasedSecretRetriever(jenkins);
        OriginRetriever originRetriever = new OriginRetriever(new DefaultConfigFileProvider(run), run, jenkinsRootUrl);
        ArtifactFileManager artifactFileManager = new ArtifactFileManager(run, launcher, listener);
        PowerShellExecutor pwsh = new PowerShellExecutor("pwsh");
        ApiConfiguration apiConfiguration = new ApiConfiguration(new URL(signStep.getApiUrl()),
                signStep.getServiceUnavailableTimeoutInSeconds(),
                signStep.getUploadAndDownloadRequestTimeoutInSeconds(),
                signStep.getWaitForCompletionTimeoutInSeconds());
        ISignPathFacadeFactory signPathFacadeFactory = new SignPathPowerShellFacadeFactory(pwsh, apiConfiguration);

        try {
            String trustedBuildSystemToken = credentialSecretRetriever.retrieveSecret(Constants.TrustedBuildSystemTokenCredentialId);
            SignPathCredentials credentials = new SignPathCredentials(signStep.getCiUserToken(), trustedBuildSystemToken);
            ISignPathFacade signPathFacade = signPathFacadeFactory.create(credentials);
            SigningRequestOriginModel originModel = originRetriever.retrieveOrigin();
            TemporaryFile unsignedArtifact = artifactFileManager.retrieveArtifact(signStep.getInputArtifactPath());

            TemporaryFile signedArtifact = signPathFacade.submitSigningRequest(new SigningRequestModel(
                    UUID.fromString(signStep.getOrganizationId()),
                    signStep.getProjectSlug(),
                    signStep.getArtifactConfigurationSlug(),
                    signStep.getSigningPolicySlug(),
                    signStep.getDescription(),
                    originModel,
                    unsignedArtifact));

            artifactFileManager.storeArtifact(signedArtifact, signStep.getOutputArtifactPath());
            return "Signing step succeeded";
        } catch (SecretNotFoundException | OriginNotRetrievableException | SignPathFacadeCallException ex) {
            return "Signing step failed: " + ex.getMessage();
        }
    }
}
