package io.jenkins.plugins.SignPath;

import io.jenkins.plugins.SignPath.ApiIntegration.Model.SigningRequestModel;
import io.jenkins.plugins.SignPath.ApiIntegration.Model.SigningRequestOriginModel;
import io.jenkins.plugins.SignPath.ApiIntegration.SignPathCredentials;
import io.jenkins.plugins.SignPath.ApiIntegration.SignPathFacade;
import io.jenkins.plugins.SignPath.ApiIntegration.SignPathFacadeFactory;
import io.jenkins.plugins.SignPath.Artifacts.ArtifactFileManager;
import io.jenkins.plugins.SignPath.Common.TemporaryFile;
import io.jenkins.plugins.SignPath.Exceptions.*;
import io.jenkins.plugins.SignPath.OriginRetrieval.OriginRetriever;
import io.jenkins.plugins.SignPath.SecretRetrieval.SecretRetriever;
import io.jenkins.plugins.SignPath.StepShared.SignPathContext;
import io.jenkins.plugins.SignPath.StepShared.SubmitSigningRequestStepInput;
import org.jenkinsci.plugins.workflow.steps.SynchronousStepExecution;

import java.io.IOException;
import java.io.PrintStream;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * The step-execution for the
 *
 * @see SubmitSigningRequestStep
 */
public class SubmitSigningRequestStepExecution extends SynchronousStepExecution<String> {
    private final PrintStream logger;
    private final SecretRetriever secretRetriever;
    private final OriginRetriever originRetriever;
    private final ArtifactFileManager artifactFileManager;
    private final SignPathFacadeFactory signPathFacadeFactory;
    private final SubmitSigningRequestStepInput input;

    protected SubmitSigningRequestStepExecution(SubmitSigningRequestStepInput input,
                                                SignPathContext context) {
        super(context.getStepContext());
        this.input = input;
        this.logger = context.getLogger();
        this.secretRetriever = context.getSecretRetriever();
        this.originRetriever = context.getOriginRetriever();
        this.artifactFileManager = context.getArtifactFileManager();
        this.signPathFacadeFactory = context.getSignPathFacadeFactory();
    }

    @Override
    protected String run() throws SignPathStepFailedException {

        logger.printf("Submitting signing request for organization:%s (waiting for completion: %s)\n", input.getOrganizationId(), input.getWaitForCompletion());

        try {
            String trustedBuildSystemToken = secretRetriever.retrieveSecret(Constants.TrustedBuildSystemTokenCredentialId);
            SignPathCredentials credentials = new SignPathCredentials(input.getCiUserToken(), trustedBuildSystemToken);
            SignPathFacade signPathFacade = signPathFacadeFactory.create(credentials);
            SigningRequestOriginModel originModel = originRetriever.retrieveOrigin();
            TemporaryFile unsignedArtifact = artifactFileManager.retrieveArtifact(input.getInputArtifactPath());

            if (input.getWaitForCompletion()) {
                TemporaryFile signedArtifact = signPathFacade.submitSigningRequest(new SigningRequestModel(
                        input.getOrganizationId(),
                        input.getProjectSlug(),
                        input.getArtifactConfigurationSlug(),
                        input.getSigningPolicySlug(),
                        input.getDescription(),
                        originModel,
                        unsignedArtifact));

                artifactFileManager.storeArtifact(signedArtifact, input.getOutputArtifactPath());
                logger.print("Signing step succeeded\n");
                return "";
            } else {
                UUID signingRequestId = signPathFacade.submitSigningRequestAsync(new SigningRequestModel(
                        input.getOrganizationId(),
                        input.getProjectSlug(),
                        input.getArtifactConfigurationSlug(),
                        input.getSigningPolicySlug(),
                        input.getDescription(),
                        originModel,
                        unsignedArtifact));

                logger.print(String.format( "Signing request created: %s\n", signingRequestId.toString()));
                return signingRequestId.toString();
            }
        } catch (SecretNotFoundException | OriginNotRetrievableException | SignPathFacadeCallException | IOException | InterruptedException | ArtifactNotFoundException | NoSuchAlgorithmException ex) {
            logger.print("\nSigning step failed: " + ex.getMessage() + "\n");
            throw new SignPathStepFailedException("Signing step failed: " + ex.getMessage(), ex);
        }
    }
}
