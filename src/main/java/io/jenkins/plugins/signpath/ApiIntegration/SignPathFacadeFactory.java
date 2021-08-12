package io.jenkins.plugins.signpath.ApiIntegration;

/**
 * A factory that creates a SignPathFacade that is bound to the given parameters
 */
public interface SignPathFacadeFactory {
    /**
     * Creates a SignPathFacade that is bound to the credentials parameter to use for authenticating against the SignPath API endpoint
     * @param credentials The credentials used for authenticating requests
     * @return The created facade
     */
    SignPathFacade create(SignPathCredentials credentials);
}