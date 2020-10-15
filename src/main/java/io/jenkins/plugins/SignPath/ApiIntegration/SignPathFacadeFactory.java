package io.jenkins.plugins.SignPath.ApiIntegration;

/**
 * A factory that creates a
 *
 * @see SignPathFacade
 * that is bound to the given parameters
 */
public interface SignPathFacadeFactory {
    /**
     * Creates a
     *
     * @param credentials the
     * @return the credential-bound
     * @see SignPathFacade
     * that is bound to the credentials parameter
     * @see SignPathCredentials
     * to use for authenticating against the SignPath Api endpoint
     * @see SignPathFacade
     */
    SignPathFacade create(SignPathCredentials credentials);
}