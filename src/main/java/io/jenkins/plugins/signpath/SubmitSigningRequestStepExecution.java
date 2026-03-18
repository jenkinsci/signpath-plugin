package io.jenkins.plugins.signpath;

import com.cloudbees.plugins.credentials.CredentialsScope;
import hudson.FilePath;
import hudson.model.TaskListener;
import hudson.util.Secret;
import io.jenkins.plugins.signpath.ApiIntegration.Model.SigningRequestWithArtifactRetrievalLinkModel;
import io.jenkins.plugins.signpath.ApiIntegration.Model.SigningRequestWithoutArtifactModel;
import io.jenkins.plugins.signpath.ApiIntegration.Model.SigningRequestOriginModel;
import io.jenkins.plugins.signpath.ApiIntegration.Model.SubmitSigningRequestWithArtifactRetrievalLinkResult;
import io.jenkins.plugins.signpath.ApiIntegration.Model.SubmitSigningRequestWithoutArtifactResult;
import io.jenkins.plugins.signpath.ApiIntegration.SignPathCredentials;
import io.jenkins.plugins.signpath.ApiIntegration.SignPathFacade;
import io.jenkins.plugins.signpath.ApiIntegration.SignPathFacadeFactory;
import io.jenkins.plugins.signpath.Artifacts.ArtifactFileManager;
import io.jenkins.plugins.signpath.Artifacts.ComputeArtifactHashCallable;
import io.jenkins.plugins.signpath.Common.TemporaryFile;
import io.jenkins.plugins.signpath.Exceptions.*;
import io.jenkins.plugins.signpath.OriginRetrieval.OriginRetriever;
import io.jenkins.plugins.signpath.SecretRetrieval.SecretRetriever;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.UUID;

/**
 * The step-execution for the
 * @see SubmitSigningRequestStep
 */
public class SubmitSigningRequestStepExecution extends SynchronousNonBlockingStepExecution<String> {
    // We do not support resuming execution and therefore can mark our fields as transient (=> not serialized)
    // If we want to support resuming, we need to remove 'transient' and make sure everything is serializable
    private transient final SubmitSigningRequestStepInput input;
    private transient final SecretRetriever secretRetriever;
    private transient final OriginRetriever originRetriever;
    private transient final ArtifactFileManager artifactFileManager;
    private transient final SignPathFacadeFactory signPathFacadeFactory;
    private transient final TaskListener taskListener;

    protected SubmitSigningRequestStepExecution(SubmitSigningRequestStepInput input,
                                                SecretRetriever secretRetriever,
                                                OriginRetriever originRetriever,
                                                ArtifactFileManager artifactFileManager,
                                                SignPathFacadeFactory signPathFacadeFactory,
                                                TaskListener taskListener,
                                                StepContext stepContext) {
        super(stepContext);
        this.input = input;
        this.secretRetriever = secretRetriever;
        this.originRetriever = originRetriever;
        this.artifactFileManager = artifactFileManager;
        this.signPathFacadeFactory = signPathFacadeFactory;
        this.taskListener = taskListener;
    }

    @Override
    protected String run() throws SignPathStepFailedException {

        PrintStream logger = taskListener.getLogger();

        logger.printf("Submitting signing request for organization: %s (waiting for completion: %s)%n", input.getOrganizationId(), input.getWaitForCompletion());

        logger.printf("[PARAM] organizationId: %s%n", input.getOrganizationId());
        logger.printf("[PARAM] projectSlug: %s%n", input.getProjectSlug());
        logger.printf("[PARAM] signingPolicySlug: %s%n", input.getSigningPolicySlug());
        if (!StringUtils.isEmpty(input.getArtifactConfigurationSlug())) {
            logger.printf("[PARAM] artifactConfigurationSlug: %s%n", input.getArtifactConfigurationSlug());
        }
        if (input.hasArtifactRetrievalUrl()) {
            logger.printf("[PARAM] inputArtifactRetrievalUrl: %s%n", input.getInputArtifactRetrievalUrl());
            if (input.getInputArtifactRetrievalHttpHeaders() != null && !input.getInputArtifactRetrievalHttpHeaders().isEmpty()) {
                logger.printf("[PARAM] inputArtifactRetrievalHttpHeaders keys: %s%n",
                        String.join(", ", input.getInputArtifactRetrievalHttpHeaders().keySet()));
            }
        }

        try {
            Secret trustedBuildSystemToken = secretRetriever.retrieveSecret(input.getTrustedBuildSystemTokenCredentialId());
            Secret apiToken = secretRetriever.retrieveSecret(input.getApiTokenCredentialId(), new CredentialsScope[]{CredentialsScope.SYSTEM, CredentialsScope.GLOBAL});
            SignPathCredentials credentials = new SignPathCredentials(apiToken, trustedBuildSystemToken);
            SignPathFacade signPathFacade = signPathFacadeFactory.create(credentials);

            // Resolve the artifact in the agent workspace
            FilePath workspace = getContext().get(FilePath.class);
            if (workspace == null) {
                throw new ArtifactNotFoundException("Could not obtain workspace from step context.");
            }
            FilePath artifactFilePath = workspace.child(input.getInputArtifactPath());
            if (!artifactFilePath.exists()) {
                throw new ArtifactNotFoundException(String.format(
                        "The artifact at path '%s' was not found in the workspace.", input.getInputArtifactPath()));
            }

            // Compute SHA-256 hash on the agent
            logger.println("Computing SHA-256 hash of artifact on agent...");
            String sha256Hex = artifactFilePath.act(new ComputeArtifactHashCallable());

            // Archive the .sha256 file (base64-encoded hash) to the Jenkins server
            byte[] sha256Bytes = Hex.decodeHex(sha256Hex);
            String sha256Base64 = Base64.getEncoder().encodeToString(sha256Bytes);
            String sha256ArtifactPath = input.getInputArtifactPath() + ".sha256";
            try (TemporaryFile hashFile = new TemporaryFile(FilenameUtils.getName(sha256ArtifactPath))) {
                Files.write(hashFile.getFile().toPath(), sha256Base64.getBytes(StandardCharsets.UTF_8));
                artifactFileManager.storeArtifact(hashFile, sha256ArtifactPath);
            }
            logger.println("SHA-256 hash file archived: " + sha256ArtifactPath);

            // Submit signing request and optionally wait for completion
            try (SigningRequestOriginModel originModel = originRetriever.retrieveOrigin()) {
                String fileName = FilenameUtils.getName(input.getInputArtifactPath());
                UUID signingRequestId;
                String webLink;

                if (input.hasArtifactRetrievalUrl()) {
                    // Retrieval link path: SignPath downloads the artifact from the provided URL
                    logger.printf("Submitting signing request with artifact retrieval URL '%s'...%n", input.getInputArtifactRetrievalUrl());
                    SigningRequestWithArtifactRetrievalLinkModel model = new SigningRequestWithArtifactRetrievalLinkModel(
                            input.getOrganizationId(),
                            fileName,
                            sha256Hex,
                            input.getProjectSlug(),
                            input.getArtifactConfigurationSlug(),
                            input.getSigningPolicySlug(),
                            input.getDescription(),
                            originModel,
                            input.getParameters(),
                            input.getInputArtifactRetrievalUrl(),
                            input.getInputArtifactRetrievalHttpHeaders());

                    SubmitSigningRequestWithArtifactRetrievalLinkResult submitResult = signPathFacade.submitSigningRequestWithArtifactRetrievalLink(model);
                    signingRequestId = submitResult.getSigningRequestId();
                    webLink = submitResult.getWebLink();
                } else {
                    // Direct upload path: artifact is uploaded from the agent to SignPath
                    SigningRequestWithoutArtifactModel model = new SigningRequestWithoutArtifactModel(
                            input.getOrganizationId(),
                            fileName,
                            sha256Hex,
                            input.getProjectSlug(),
                            input.getArtifactConfigurationSlug(),
                            input.getSigningPolicySlug(),
                            input.getDescription(),
                            originModel,
                            input.getParameters());

                    SubmitSigningRequestWithoutArtifactResult submitResult = signPathFacade.submitSigningRequestWithoutArtifact(model);
                    signingRequestId = submitResult.getSigningRequestId();
                    webLink = submitResult.getWebLink();

                    // Upload the artifact to SignPath
                    logger.printf("Uploading artifact '%s' to SignPath...%n", input.getInputArtifactPath());
                    try (InputStream artifactStream = artifactFilePath.read()) {
                        signPathFacade.uploadUnsignedArtifact(submitResult.getUploadLink(), artifactStream);
                    }
                }

                if (webLink != null && !webLink.isEmpty()) {
                    logger.printf("Signing request URL: %s%n", webLink);
                } else {
                    logger.println("WARNING: Signing request URL was not provided by the server.");
                }

                if (input.getWaitForCompletion()) {
                    try (TemporaryFile signedArtifact = signPathFacade.getSignedArtifact(input.getOrganizationId(), signingRequestId)) {
                        artifactFileManager.storeArtifact(signedArtifact, input.getOutputArtifactPath());
                    }
                    logger.println("Signing step succeeded");
                }

                return signingRequestId.toString();
            }
        } catch (SecretNotFoundException | OriginNotRetrievableException | SignPathFacadeCallException |
                 ArtifactNotFoundException | IOException | InterruptedException | NoSuchAlgorithmException |
                 DecoderException ex) {
            logger.printf("%nSigning step failed: %s%n", ex.getMessage());
            throw new SignPathStepFailedException("Signing step failed: " + ex.getMessage(), ex);
        }
    }
}
