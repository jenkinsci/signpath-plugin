package io.jenkins.plugins.SignPath.ApiIntegration;

public interface ISignPathFacadeFactory {
    ISignPathFacade create(SignPathCredentials credentials);
}
