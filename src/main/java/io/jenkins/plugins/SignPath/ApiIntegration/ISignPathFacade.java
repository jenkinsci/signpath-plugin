package io.jenkins.plugins.SignPath.ApiIntegration;

import io.jenkins.plugins.SignPath.ApiIntegration.Model.SigningRequestModel;
import io.jenkins.plugins.SignPath.Common.TemporaryFile;

import java.util.UUID;

public interface ISignPathFacade {
 TemporaryFile submitSigningRequest(SigningRequestModel submitModel);
 void submitSigningRequestAsync(SigningRequestModel submitModel);
 TemporaryFile getSignedArtifact(UUID organizationId, UUID signingRequestID);
}
