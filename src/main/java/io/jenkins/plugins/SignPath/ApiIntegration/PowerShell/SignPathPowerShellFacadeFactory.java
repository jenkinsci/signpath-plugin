package io.jenkins.plugins.SignPath.ApiIntegration.PowerShell;

import io.jenkins.plugins.SignPath.ApiIntegration.ApiConfiguration;
import io.jenkins.plugins.SignPath.ApiIntegration.ISignPathFacade;
import io.jenkins.plugins.SignPath.ApiIntegration.ISignPathFacadeFactory;
import io.jenkins.plugins.SignPath.ApiIntegration.SignPathCredentials;

public class SignPathPowerShellFacadeFactory implements ISignPathFacadeFactory {
    private IPowerShellExecutor powerShellExecutor;
    private ApiConfiguration apiConfiguration;

    public SignPathPowerShellFacadeFactory(IPowerShellExecutor powerShellExecutor, ApiConfiguration apiConfiguration){

        this.powerShellExecutor = powerShellExecutor;
        this.apiConfiguration = apiConfiguration;
    }

    @Override
    public ISignPathFacade create(SignPathCredentials credentials) {
        return new SignPathPowerShellFacade(powerShellExecutor, credentials, apiConfiguration);
    }
}
