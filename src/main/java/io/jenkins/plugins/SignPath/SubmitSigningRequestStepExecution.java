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
import io.jenkins.plugins.SignPath.Exceptions.SignPathStepFailedException;
import io.jenkins.plugins.SignPath.OriginRetrieval.IOriginRetriever;
import io.jenkins.plugins.SignPath.SecretRetrieval.ISecretRetriever;
import io.jenkins.plugins.SignPath.StepInputParser.SigningRequestStepInput;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousStepExecution;

import java.io.IOException;
import java.io.PrintStream;
import java.util.UUID;

public class SubmitSigningRequestStepExecution extends SynchronousStepExecution<String> {
    private final PrintStream logger;
    private final ISecretRetriever secretRetriever;
    private final IOriginRetriever originRetriever;
    private final IArtifactFileManager artifactFileManager;
    private final ISignPathFacadeFactory signPathFacadeFactory;
    private SigningRequestStepInput input;

    protected SubmitSigningRequestStepExecution(SigningRequestStepInput input,
                                                StepContext context,
                                                PrintStream logger,
                                                ISecretRetriever secretRetriever,
                                                IOriginRetriever originRetriever,
                                                IArtifactFileManager artifactFileManager,
                                                ISignPathFacadeFactory signPathFacadeFactory) {
        super(context);
        this.input = input;
        this.logger = logger;
        this.secretRetriever = secretRetriever;
        this.originRetriever = originRetriever;
        this.artifactFileManager = artifactFileManager;
        this.signPathFacadeFactory = signPathFacadeFactory;
    }

    @Override
    protected String run() throws SignPathStepFailedException {

        logger.printf("SubmitSigningRequestStepExecution organizationId:%s waitForCompletion: %s\n", input.getOrganizationId(), input.getWaitForCompletion());

        try {
            String trustedBuildSystemToken = secretRetriever.retrieveSecret(Constants.TrustedBuildSystemTokenCredentialId);
            SignPathCredentials credentials = new SignPathCredentials(input.getCiUserToken(), trustedBuildSystemToken);
            ISignPathFacade signPathFacade = signPathFacadeFactory.create(credentials);
            SigningRequestOriginModel originModel = originRetriever.retrieveOrigin();
            TemporaryFile unsignedArtifact = artifactFileManager.retrieveArtifact(input.getInputArtifactPath());

            TemporaryFile signedArtifact = signPathFacade.submitSigningRequest(new SigningRequestModel(
                    input.getOrganizationId(),
                    input.getProjectSlug(),
                    input.getArtifactConfigurationSlug(),
                    input.getSigningPolicySlug(),
                    input.getDescription(),
                    originModel,
                    unsignedArtifact));

            artifactFileManager.storeArtifact(signedArtifact, input.getOutputArtifactPath());
            logger.printf("\nSigning step succeeded\n");
            return "";
        } catch (SecretNotFoundException | OriginNotRetrievableException | SignPathFacadeCallException | IOException | InterruptedException ex) {
            logger.printf("\nSigning step failed: " + ex.getMessage() + "\n");
            throw new SignPathStepFailedException("Signing step failed: " + ex.getMessage(), ex);
        }
    }
}
