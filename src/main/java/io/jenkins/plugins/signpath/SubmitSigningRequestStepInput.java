package io.jenkins.plugins.signpath;

import lombok.Getter;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

/**
 * Holds all input specific to the
 *
 * @see io.jenkins.plugins.signpath.SubmitSigningRequestStep
 */
public class SubmitSigningRequestStepInput implements Serializable {
    private static final long serialVersionUID = 1L;

    @Getter
    private final UUID organizationId;
    @Getter
    private final String apiTokenCredentialId;
    @Getter
    private final String projectSlug;
    @Getter
    private final String artifactConfigurationSlug;
    @Getter
    private final String signingPolicySlug;
    @Getter
    private final String inputArtifactPath;
    @Getter
    private final String description;
    @Getter
    private final String outputArtifactPath;
    private final boolean waitForCompletion;
    @Getter
    private final Map<String, String> parameters;
    @Getter
    private final String inputArtifactRetrievalUrl;
    @Getter
    private final Map<String, String> inputArtifactRetrievalHttpHeaders;

    public SubmitSigningRequestStepInput(UUID organizationId,
                                         String apiTokenCredentialId,
                                         String projectSlug,
                                         String artifactConfigurationSlug,
                                         String signingPolicySlug,
                                         String inputArtifactPath,
                                         String description,
                                         String outputArtifactPath,
                                         Map<String, String> parameters,
                                         boolean waitForCompletion,
                                         String inputArtifactRetrievalUrl,
                                         Map<String, String> inputArtifactRetrievalHttpHeaders) {
        this.organizationId = organizationId;
        this.apiTokenCredentialId = apiTokenCredentialId;
        this.projectSlug = projectSlug;
        this.artifactConfigurationSlug = artifactConfigurationSlug;
        this.signingPolicySlug = signingPolicySlug;
        this.inputArtifactPath = inputArtifactPath;
        this.description = description;
        this.outputArtifactPath = outputArtifactPath;
        this.parameters = parameters;
        this.waitForCompletion = waitForCompletion;
        this.inputArtifactRetrievalUrl = inputArtifactRetrievalUrl;
        this.inputArtifactRetrievalHttpHeaders = inputArtifactRetrievalHttpHeaders;
    }

    public boolean getWaitForCompletion() {
        return waitForCompletion;
    }

    public boolean hasOutputArtifactPath() {
        return outputArtifactPath != null && !outputArtifactPath.isEmpty();
    }

    public boolean hasArtifactRetrievalUrl() {
        return inputArtifactRetrievalUrl != null && !inputArtifactRetrievalUrl.isEmpty();
    }
}
