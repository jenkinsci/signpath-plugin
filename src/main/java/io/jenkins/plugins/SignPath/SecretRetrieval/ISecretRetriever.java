package io.jenkins.plugins.SignPath.SecretRetrieval;

import io.jenkins.plugins.SignPath.Exceptions.SecretNotFoundException;

public interface ISecretRetriever {
    String retrieveSecret(String id) throws SecretNotFoundException;
}
