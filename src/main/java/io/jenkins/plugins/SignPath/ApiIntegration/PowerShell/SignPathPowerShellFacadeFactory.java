package io.jenkins.plugins.SignPath.ApiIntegration.PowerShell;

import io.jenkins.plugins.SignPath.ApiIntegration.ApiConfiguration;
import io.jenkins.plugins.SignPath.ApiIntegration.SignPathCredentials;
import io.jenkins.plugins.SignPath.ApiIntegration.SignPathFacade;
import io.jenkins.plugins.SignPath.ApiIntegration.SignPathFacadeFactory;

/**
 * @see SignPathFacadeFactory
 * that produces a
 * @see SignPathPowerShellFacade
 */
public class SignPathPowerShellFacadeFactory implements SignPathFacadeFactory {
    private final PowerShellExecutor powerShellExecutor;
    private final ApiConfiguration apiConfiguration;

    public SignPathPowerShellFacadeFactory(PowerShellExecutor powerShellExecutor, ApiConfiguration apiConfiguration) {
        this.powerShellExecutor = powerShellExecutor;
        this.apiConfiguration = apiConfiguration;
    }

    @Override
    public SignPathFacade create(SignPathCredentials credentials) {
        return new SignPathPowerShellFacade(powerShellExecutor, credentials, apiConfiguration);
    }
}