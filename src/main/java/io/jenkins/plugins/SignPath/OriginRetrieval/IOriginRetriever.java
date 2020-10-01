package io.jenkins.plugins.SignPath.OriginRetrieval;

import io.jenkins.plugins.SignPath.ApiIntegration.Model.SigningRequestOriginModel;
import io.jenkins.plugins.SignPath.Exceptions.OriginNotRetrievableException;

import java.io.IOException;

public interface IOriginRetriever {
    SigningRequestOriginModel retrieveOrigin() throws IOException, OriginNotRetrievableException;
}
