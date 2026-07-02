package io.jenkins.plugins.signpath.ApiIntegration;

/**
 * A factory that creates a PipelineConnectorFacade that is bound to the given parameters
 */
public interface PipelineConnectorFacadeFactory {
    /**
     * Creates a PipelineConnectorFacade that is bound to the credentials parameter to use for authenticating against the SignPath Pipeline Connector
     * @param credentials The credentials used for authenticating requests
     * @return The created facade
     */
    PipelineConnectorFacade create(SignPathCredentials credentials);
}
