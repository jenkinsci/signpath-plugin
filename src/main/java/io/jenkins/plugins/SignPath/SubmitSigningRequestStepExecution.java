package io.jenkins.plugins.SignPath;

import io.jenkins.plugins.SignPath.ApiIntegration.ISignPathFacade;
import io.jenkins.plugins.SignPath.ApiIntegration.ISignPathFacadeFactory;
import io.jenkins.plugins.SignPath.ApiIntegration.Model.SigningRequestModel;
import io.jenkins.plugins.SignPath.ApiIntegration.Model.SigningRequestOriginModel;
import io.jenkins.plugins.SignPath.ApiIntegration.SignPathCredentials;
import io.jenkins.plugins.SignPath.Artifacts.IArtifactFileManager;
import io.jenkins.plugins.SignPath.Common.TemporaryFile;
import io.jenkins.plugins.SignPath.Exceptions.OriginNotRetrievableException;
import io.jenkins.plugins.SignPath.Exceptions.SecretNotFoundException;
import io.jenkins.plugins.SignPath.Exceptions.SignPathFacadeCallException;
import io.jenkins.plugins.SignPath.OriginRetrieval.IOriginRetriever;
import io.jenkins.plugins.SignPath.SecretRetrieval.ISecretRetriever;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousStepExecution;

import java.io.PrintStream;
import java.util.UUID;

public class SubmitSigningRequestStepExecution extends SynchronousStepExecution<String> {
    private final SubmitSigningRequestStep signStep;
    private final PrintStream logger;
    private final ISecretRetriever secretRetriever;
    private final IOriginRetriever originRetriever;
    private final IArtifactFileManager artifactFileManager;
    private final ISignPathFacadeFactory signPathFacadeFactory;

    protected SubmitSigningRequestStepExecution(SubmitSigningRequestStep signStep,
                                                StepContext context,
                                                PrintStream logger,
                                                ISecretRetriever secretRetriever,
                                                IOriginRetriever originRetriever,
                                                IArtifactFileManager artifactFileManager,
                                                ISignPathFacadeFactory signPathFacadeFactory) {
        super(context);
        this.signStep = signStep;
        this.logger = logger;
        this.secretRetriever = secretRetriever;
        this.originRetriever = originRetriever;
        this.artifactFileManager = artifactFileManager;
        this.signPathFacadeFactory = signPathFacadeFactory;
    }

    @Override
    protected String run() throws Exception {

        logger.printf("SubmitSigningRequestStepExecution organizationId:%s waitForCompletion: %s\n", signStep.getOrganizationId(), signStep.getWaitForCompletion());

        try {
            String trustedBuildSystemToken = secretRetriever.retrieveSecret(Constants.TrustedBuildSystemTokenCredentialId);
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
            logger.printf("\nSigning step succeeded\n");
            return "";
        } catch (SecretNotFoundException | OriginNotRetrievableException | SignPathFacadeCallException ex) {
            logger.printf("\nSigning step failed: " + ex.getMessage()+"\n");
            throw new Exception("Signing step failed: " + ex.getMessage(), ex);
        }
    }
}
