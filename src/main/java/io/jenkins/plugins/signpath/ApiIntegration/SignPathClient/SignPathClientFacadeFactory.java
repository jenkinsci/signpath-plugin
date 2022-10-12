package io.jenkins.plugins.signpath.ApiIntegration.SignPathClient;

import io.jenkins.plugins.signpath.ApiIntegration.ApiConfiguration;
import io.jenkins.plugins.signpath.ApiIntegration.SignPathCredentials;
import io.jenkins.plugins.signpath.ApiIntegration.SignPathFacade;
import io.jenkins.plugins.signpath.ApiIntegration.SignPathFacadeFactory;

import java.io.PrintStream;

/**
 * @see SignPathFacadeFactory
 * that produces a
 * @see SignPathPowerShellFacade
 */
public class SignPathClientFacadeFactory implements SignPathFacadeFactory {
    private final ApiConfiguration apiConfiguration;
    private final PrintStream logger;

    public SignPathClientFacadeFactory(ApiConfiguration apiConfiguration, PrintStream logger) {
        this.apiConfiguration = apiConfiguration;
        this.logger = logger;
    }

    @Override
    public SignPathFacade create(SignPathCredentials credentials) {
        return new SignPathClientFacade(credentials, apiConfiguration, logger);
    }
}