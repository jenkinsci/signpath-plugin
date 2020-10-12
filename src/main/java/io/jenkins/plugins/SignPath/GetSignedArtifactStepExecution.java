package io.jenkins.plugins.SignPath;

import io.jenkins.plugins.SignPath.ApiIntegration.ISignPathFacade;
import io.jenkins.plugins.SignPath.ApiIntegration.ISignPathFacadeFactory;
import io.jenkins.plugins.SignPath.ApiIntegration.SignPathCredentials;
import io.jenkins.plugins.SignPath.Artifacts.IArtifactFileManager;
import io.jenkins.plugins.SignPath.Common.TemporaryFile;
import io.jenkins.plugins.SignPath.Exceptions.SecretNotFoundException;
import io.jenkins.plugins.SignPath.Exceptions.SignPathFacadeCallException;
import io.jenkins.plugins.SignPath.Exceptions.SignPathStepFailedException;
import io.jenkins.plugins.SignPath.SecretRetrieval.ISecretRetriever;
import io.jenkins.plugins.SignPath.StepShared.GetSignedArtifactStepInput;
import io.jenkins.plugins.SignPath.StepShared.SignPathContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousStepExecution;

import java.io.IOException;
import java.io.PrintStream;

public class GetSignedArtifactStepExecution extends SynchronousStepExecution<String> {
    private final PrintStream logger;
    private final ISecretRetriever secretRetriever;
    private final IArtifactFileManager artifactFileManager;
    private final ISignPathFacadeFactory signPathFacadeFactory;
    private final GetSignedArtifactStepInput input;

    protected GetSignedArtifactStepExecution(GetSignedArtifactStepInput input,
                                             SignPathContext signPathContext) {
        super(signPathContext.getStepContext());
        this.input = input;
        this.logger = signPathContext.getLogger();
        this.secretRetriever = signPathContext.getSecretRetriever();
        this.artifactFileManager = signPathContext.getArtifactFileManager();
        this.signPathFacadeFactory = signPathContext.getSignPathFacadeFactory();
    }

    @Override
    protected String run() throws SignPathStepFailedException {
        logger.printf("GetSignedArtifactStepExecution organizationId: %s signingRequestId: %s\n", input.getOrganizationId(), input.getSigningRequestId());

        try {
            String trustedBuildSystemToken = secretRetriever.retrieveSecret(Constants.TrustedBuildSystemTokenCredentialId);
            SignPathCredentials credentials = new SignPathCredentials(input.getCiUserToken(), trustedBuildSystemToken);
            ISignPathFacade signPathFacade = signPathFacadeFactory.create(credentials);
            TemporaryFile signedArtifact = signPathFacade.getSignedArtifact(input.getOrganizationId(), input.getSigningRequestId());

            artifactFileManager.storeArtifact(signedArtifact, input.getOutputArtifactPath());
            logger.print("\nSigning step succeeded\n");
            return "";
        } catch (SecretNotFoundException | SignPathFacadeCallException | IOException | InterruptedException ex) {
            logger.print("\nSigning step failed: " + ex.getMessage() + "\n");
            throw new SignPathStepFailedException("Signing step failed: " + ex.getMessage(), ex);
        }
    }
}
