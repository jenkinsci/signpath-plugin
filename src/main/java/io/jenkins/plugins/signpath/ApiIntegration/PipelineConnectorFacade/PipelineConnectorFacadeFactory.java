package io.jenkins.plugins.signpath.ApiIntegration.PipelineConnectorFacade;

import io.jenkins.plugins.signpath.ApiIntegration.ApiConfiguration;
import io.jenkins.plugins.signpath.ApiIntegration.IPipelineConnectorFacade;
import io.jenkins.plugins.signpath.ApiIntegration.IPipelineConnectorFacadeFactory;
import io.jenkins.plugins.signpath.ApiIntegration.SignPathCredentials;
import io.signpath.signpathclient.SignPathClientSimpleLogger;

/**
 * @see IPipelineConnectorFacadeFactory
 */
public class PipelineConnectorFacadeFactory implements IPipelineConnectorFacadeFactory {
    private final ApiConfiguration apiConfiguration;
    private final SignPathClientSimpleLogger logger;

    public PipelineConnectorFacadeFactory(ApiConfiguration apiConfiguration, SignPathClientSimpleLogger logger) {
        this.apiConfiguration = apiConfiguration;
        this.logger = logger;
    }

    @Override
    public IPipelineConnectorFacade create(SignPathCredentials credentials) {
        return new PipelineConnectorFacade(credentials, apiConfiguration, logger);
    }
}
