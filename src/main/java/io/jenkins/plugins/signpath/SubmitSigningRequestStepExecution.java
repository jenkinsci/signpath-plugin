package io.jenkins.plugins.signpath;

import hudson.util.Secret;
import io.jenkins.plugins.signpath.ApiIntegration.Model.SigningRequestModel;
import io.jenkins.plugins.signpath.ApiIntegration.Model.SigningRequestOriginModel;
import io.jenkins.plugins.signpath.ApiIntegration.Model.SubmitSigningRequestResult;
import io.jenkins.plugins.signpath.ApiIntegration.SignPathCredentials;
import io.jenkins.plugins.signpath.ApiIntegration.SignPathFacade;
import io.jenkins.plugins.signpath.ApiIntegration.SignPathFacadeFactory;
import io.jenkins.plugins.signpath.Artifacts.ArtifactFileManager;
import io.jenkins.plugins.signpath.Common.TemporaryFile;
import io.jenkins.plugins.signpath.Exceptions.*;
import io.jenkins.plugins.signpath.OriginRetrieval.OriginRetriever;
import io.jenkins.plugins.signpath.SecretRetrieval.SecretRetriever;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousStepExecution;

import java.io.IOException;
import java.io.PrintStream;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * The step-execution for the
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
                                                SecretRetriever secretRetriever,
                                                OriginRetriever originRetriever,
                                                ArtifactFileManager artifactFileManager,
                                                SignPathFacadeFactory signPathFacadeFactory,
                                                PrintStream logger,
                                                StepContext stepContext) {
        super(stepContext);
        this.input = input;
        this.logger = logger;
        this.secretRetriever = secretRetriever;
        this.originRetriever = originRetriever;
        this.artifactFileManager = artifactFileManager;
        this.signPathFacadeFactory = signPathFacadeFactory;
    }

    @Override
    protected String run() throws SignPathStepFailedException {

        logger.printf("Submitting signing request for organization: %s (waiting for completion: %s)%n", input.getOrganizationId(), input.getWaitForCompletion());

        try {
            Secret trustedBuildSystemToken = secretRetriever.retrieveSecret(input.getTrustedBuildSystemTokenCredentialId());
            Secret ciUserToken = secretRetriever.retrieveSecret(input.getCiUserTokenCredentialId());
            SignPathCredentials credentials = new SignPathCredentials(ciUserToken, trustedBuildSystemToken);
            SignPathFacade signPathFacade = signPathFacadeFactory.create(credentials);
            try(SigningRequestOriginModel originModel = originRetriever.retrieveOrigin()) {
                try (TemporaryFile unsignedArtifact = artifactFileManager.retrieveArtifact(input.getInputArtifactPath())) {
                    SigningRequestModel model = new SigningRequestModel(
                            input.getOrganizationId(),
                            input.getProjectSlug(),
                            input.getArtifactConfigurationSlug(),
                            input.getSigningPolicySlug(),
                            input.getDescription(),
                            originModel,
                            unsignedArtifact);

                    if (input.getWaitForCompletion()) {
                        try (SubmitSigningRequestResult result = signPathFacade.submitSigningRequest(model)) {
                            artifactFileManager.storeArtifact(result.getSignedArtifact(), input.getOutputArtifactPath());
                            logger.println("Signing step succeeded");
                            return result.getSigningRequestId().toString();
                        }
                    } else {
                        UUID signingRequestId = signPathFacade.submitSigningRequestAsync(model);

                        logger.printf("Signing request created: %s%n", signingRequestId.toString());
                        return signingRequestId.toString();
                    }
                }
            }
        } catch (SecretNotFoundException | OriginNotRetrievableException | SignPathFacadeCallException | IOException | InterruptedException | ArtifactNotFoundException | NoSuchAlgorithmException ex) {
            logger.printf("%nSigning step failed: %s%n", ex.getMessage());
            throw new SignPathStepFailedException("Signing step failed: " + ex.getMessage(), ex);
        }
    }
}
