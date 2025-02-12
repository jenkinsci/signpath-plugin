package io.jenkins.plugins.signpath.ApiIntegration.SignPathClient;

import io.jenkins.plugins.signpath.ApiIntegration.ApiConfiguration;
import io.jenkins.plugins.signpath.ApiIntegration.SignPathCredentials;
import io.jenkins.plugins.signpath.ApiIntegration.SignPathFacade;
import io.jenkins.plugins.signpath.ApiIntegration.SignPathFacadeFactory;
import io.signpath.signpathclient.SignPathClientSimpleLogger;

import java.io.PrintStream;

/**
 * @see SignPathFacadeFactory
 */
public class SignPathClientFacadeFactory implements SignPathFacadeFactory {
    private final ApiConfiguration apiConfiguration;
    private final SignPathClientSimpleLogger logger;

    public SignPathClientFacadeFactory(ApiConfiguration apiConfiguration, SignPathClientSimpleLogger logger) {
        this.apiConfiguration = apiConfiguration;
        this.logger = logger;
    }

    @Override
    public SignPathFacade create(SignPathCredentials credentials) {
        return new SignPathClientFacade(credentials, apiConfiguration, logger);
    }
}