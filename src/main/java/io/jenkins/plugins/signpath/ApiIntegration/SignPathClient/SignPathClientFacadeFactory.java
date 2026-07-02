package io.jenkins.plugins.signpath.ApiIntegration.SignPathClient;

import io.jenkins.plugins.signpath.ApiIntegration.ApiConfiguration;
import io.jenkins.plugins.signpath.ApiIntegration.PipelineConnectorFacade;
import io.jenkins.plugins.signpath.ApiIntegration.PipelineConnectorFacadeFactory;
import io.jenkins.plugins.signpath.ApiIntegration.SignPathCredentials;
import io.signpath.signpathclient.SignPathClientSimpleLogger;

/**
 * @see PipelineConnectorFacadeFactory
 */
public class SignPathClientFacadeFactory implements PipelineConnectorFacadeFactory {
    private final ApiConfiguration apiConfiguration;
    private final SignPathClientSimpleLogger logger;

    public SignPathClientFacadeFactory(ApiConfiguration apiConfiguration, SignPathClientSimpleLogger logger) {
        this.apiConfiguration = apiConfiguration;
        this.logger = logger;
    }

    @Override
    public PipelineConnectorFacade create(SignPathCredentials credentials) {
        return new SignPathClientFacade(credentials, apiConfiguration, logger);
    }
}
