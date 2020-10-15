package io.jenkins.plugins.SignPath.OriginRetrieval;

import io.jenkins.plugins.SignPath.ApiIntegration.Model.SigningRequestOriginModel;
import io.jenkins.plugins.SignPath.Exceptions.OriginNotRetrievableException;

import java.io.IOException;

/**
 * Retrieves origin-data from the current build
 */
public interface OriginRetriever {
    /**
     * retrieves all origin-data required to fulfill the
     *
     * @return the origin-data that was retrieved
     * @throws IOException                   occurs if the build settings file is not readable
     * @throws OriginNotRetrievableException occurs if the origin is not retrievable
     * @see io.jenkins.plugins.SignPath.ApiIntegration.SignPathFacade interface
     */
    SigningRequestOriginModel retrieveOrigin() throws IOException, OriginNotRetrievableException;
}
