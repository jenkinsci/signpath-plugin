package io.jenkins.plugins.signpath;

import com.cloudbees.plugins.credentials.CredentialsScope;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.Secret;
import io.jenkins.plugins.signpath.ApiIntegration.Model.ConnectorSigningRequestModel;
import io.jenkins.plugins.signpath.ApiIntegration.Model.SubmitSigningRequestResult;
import io.jenkins.plugins.signpath.ApiIntegration.IPipelineConnectorFacade;
import io.jenkins.plugins.signpath.ApiIntegration.IPipelineConnectorFacadeFactory;
import io.jenkins.plugins.signpath.ApiIntegration.SignPathCredentials;
import io.jenkins.plugins.signpath.Artifacts.ArtifactFileManager;
import io.jenkins.plugins.signpath.Artifacts.ComputeArtifactHashCallable;
import io.jenkins.plugins.signpath.Common.TemporaryFile;
import io.jenkins.plugins.signpath.Exceptions.*;
import io.jenkins.plugins.signpath.SecretRetrieval.SecretRetriever;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;

import java.io.FileInputStream;
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
@SuppressFBWarnings(value = {"SE_NO_SERIALVERSIONID", "SE_TRANSIENT_FIELD_NOT_RESTORED"},
        justification = "Resume is not supported; the execution is never serialized, so all fields are deliberately transient.")
public class SubmitSigningRequestStepExecution extends SynchronousNonBlockingStepExecution<String> {
    // We do not support resuming execution and therefore can mark our fields as transient (=> not serialized)
    // If we want to support resuming, we need to remove 'transient' and make sure everything is serializable
    private transient final SubmitSigningRequestStepInput input;
    private transient final SecretRetriever secretRetriever;
    private transient final ArtifactFileManager artifactFileManager;
    private transient final IPipelineConnectorFacadeFactory pipelineConnectorFacadeFactory;
    private transient final TaskListener taskListener;

    protected SubmitSigningRequestStepExecution(SubmitSigningRequestStepInput input,
                                                SecretRetriever secretRetriever,
                                                ArtifactFileManager artifactFileManager,
                                                IPipelineConnectorFacadeFactory pipelineConnectorFacadeFactory,
                                                TaskListener taskListener,
                                                StepContext stepContext) {
        super(stepContext);
        this.input = input;
        this.secretRetriever = secretRetriever;
        this.artifactFileManager = artifactFileManager;
        this.pipelineConnectorFacadeFactory = pipelineConnectorFacadeFactory;
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
            Secret apiToken = secretRetriever.retrieveSecret(input.getApiTokenCredentialId(), new CredentialsScope[]{CredentialsScope.SYSTEM, CredentialsScope.GLOBAL});
            SignPathCredentials credentials = new SignPathCredentials(apiToken);
            IPipelineConnectorFacade pipelineConnectorFacade = pipelineConnectorFacadeFactory.create(credentials);

            // Resolve the build identity so the connector can locate the build and its archived artifacts.
            Run<?, ?> run = getContext().get(Run.class);
            if (run == null) {
                throw new ArtifactNotFoundException("Could not obtain the build from the step context.");
            }
            String jobFullName = run.getParent().getFullName();
            int buildNumber = run.getNumber();
            logger.printf("[PARAM] jobFullName: %s%n", jobFullName);
            logger.printf("[PARAM] buildNumber: %d%n", buildNumber);

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

            // Archive the .sha256 sidecar (base64-encoded hash) so the connector can read the expected hash.
            byte[] sha256Bytes = Hex.decodeHex(sha256Hex);
            String sha256Base64 = Base64.getEncoder().encodeToString(sha256Bytes);
            String sha256ArtifactPath = input.getInputArtifactPath() + ".sha256";
            try (TemporaryFile hashFile = new TemporaryFile(FilenameUtils.getName(sha256ArtifactPath))) {
                Files.write(hashFile.getFile().toPath(), sha256Base64.getBytes(StandardCharsets.UTF_8));
                artifactFileManager.storeArtifact(hashFile, sha256ArtifactPath);
            }
            logger.println("SHA-256 hash file archived: " + sha256ArtifactPath);

            ConnectorSigningRequestModel model = ConnectorSigningRequestModel.builder()
                    .organizationId(input.getOrganizationId())
                    .jobFullName(jobFullName)
                    .buildNumber(buildNumber)
                    .sha256ArtifactPath(sha256ArtifactPath)
                    .projectSlug(input.getProjectSlug())
                    .artifactConfigurationSlug(input.getArtifactConfigurationSlug())
                    .signingPolicySlug(input.getSigningPolicySlug())
                    .description(input.getDescription())
                    .parameters(input.getParameters())
                    .inputArtifactRetrievalUrl(input.getInputArtifactRetrievalUrl())
                    .inputArtifactRetrievalHttpHeaders(input.getInputArtifactRetrievalHttpHeaders())
                    .build();

            if (input.hasArtifactRetrievalUrl()) {
                logger.printf("Submitting signing request with artifact retrieval URL '%s'...%n", input.getInputArtifactRetrievalUrl());
            } else {
                logger.println("Submitting signing request...");
            }

            SubmitSigningRequestResult submitResult = pipelineConnectorFacade.submitSigningRequest(model);
            UUID signingRequestId = submitResult.getSigningRequestId();
            String webLink = submitResult.getWebLink();

            if (webLink != null && !webLink.isEmpty()) {
                logger.printf("Signing request URL: %s%n", webLink);
            } else {
                logger.println("WARNING: Signing request URL was not provided by the server.");
            }

            // In retrieval-link mode SignPath downloads the artifact itself, otherwise the plugin uploads it
            // to the connector using the upload link returned by the submit call.
            if (!input.hasArtifactRetrievalUrl()) {
                logger.printf("Uploading unsigned artifact '%s'...%n", input.getInputArtifactPath());
                // The artifact may live on a remote agent, so it is copied to a temporary file on the
                // controller from where it can be uploaded.
                try (TemporaryFile unsignedArtifact = new TemporaryFile(FilenameUtils.getName(input.getInputArtifactPath()))) {
                    try (InputStream unsignedArtifactStream = artifactFilePath.read()) {
                        unsignedArtifact.copyFrom(unsignedArtifactStream);
                    }
                    pipelineConnectorFacade.uploadUnsignedArtifact(
                            input.getOrganizationId(),
                            signingRequestId,
                            submitResult.getArtifactUploadLink(),
                            unsignedArtifact.getFile());
                }
            }

            // waitForFinalSigningRequestStatus is skipped when outputArtifactPath is set because
            // getSignedArtifact below already waits for the final status internally.
            if (input.getWaitForCompletion() && !input.hasOutputArtifactPath()) {
                pipelineConnectorFacade.waitForFinalSigningRequestStatus(input.getOrganizationId(), signingRequestId);
            }

            if (input.hasOutputArtifactPath()) {
                // signedArtifact is a temporary download buffer on the controller, the try block ensures it is
                // cleaned up after its contents are copied to the persistent workspace file at outputArtifactPath.
                try (TemporaryFile signedArtifact = pipelineConnectorFacade.getSignedArtifact(input.getOrganizationId(), signingRequestId)) {
                    FilePath outputPath = workspace.child(input.getOutputArtifactPath());
                    try (InputStream signedArtifactStream = new FileInputStream(signedArtifact.getFile())) {
                        outputPath.copyFrom(signedArtifactStream);
                    }
                }
            }

            if (input.getWaitForCompletion()) {
                logger.println("Signing step succeeded");
            }

            return signingRequestId.toString();
        } catch (SecretNotFoundException | PipelineConnectorFacadeCallException |
                 ArtifactNotFoundException | IOException | InterruptedException | NoSuchAlgorithmException |
                 DecoderException ex) {
            logger.printf("%nSigning step failed: %s%n", ex.getMessage());
            throw new SignPathStepFailedException("Signing step failed: " + ex.getMessage(), ex);
        }
    }
}
