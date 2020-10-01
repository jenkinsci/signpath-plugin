package io.jenkins.plugins.SignPath.ApiIntegration;

import io.jenkins.plugins.SignPath.ApiIntegration.Model.SigningRequestModel;
import io.jenkins.plugins.SignPath.Common.TemporaryFile;

import java.util.UUID;

public class SignPathPowerShellFacade implements ISignPathFacade {

    private ApiConfiguration apiConfiguration;
    private SignPathCredentials credentials;

    public SignPathPowerShellFacade(SignPathCredentials credentials, ApiConfiguration apiConfiguration){
        this.credentials = credentials;
        this.apiConfiguration = apiConfiguration;
    }

    @Override
    public TemporaryFile submitSigningRequest(SigningRequestModel submitModel) {
        return null;
    }

    @Override
    public void submitSigningRequestAsync(SigningRequestModel submitModel) {

    }

    @Override
    public TemporaryFile getSignedArtifact(UUID organizationId, UUID signingRequestID) {
        return null;
    }
}
