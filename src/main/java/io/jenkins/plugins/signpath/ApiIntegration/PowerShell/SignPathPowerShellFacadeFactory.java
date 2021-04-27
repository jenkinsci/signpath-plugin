package io.jenkins.plugins.signpath.ApiIntegration.PowerShell;

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
public class SignPathPowerShellFacadeFactory implements SignPathFacadeFactory {
    private final PowerShellExecutor powerShellExecutor;
    private final ApiConfiguration apiConfiguration;
    private final PrintStream logger;

    public SignPathPowerShellFacadeFactory(PowerShellExecutor powerShellExecutor, ApiConfiguration apiConfiguration, PrintStream logger) {
        this.powerShellExecutor = powerShellExecutor;
        this.apiConfiguration = apiConfiguration;
        this.logger = logger;
    }

    @Override
    public SignPathFacade create(SignPathCredentials credentials) {
        return new SignPathPowerShellFacade(powerShellExecutor, credentials, apiConfiguration, logger);
    }
}