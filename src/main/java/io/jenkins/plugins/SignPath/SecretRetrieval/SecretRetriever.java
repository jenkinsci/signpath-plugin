package io.jenkins.plugins.SignPath.SecretRetrieval;

import io.jenkins.plugins.SignPath.Exceptions.SecretNotFoundException;

/**
 * A utility to retrieve secrets from the current jenkins master node
 */
public interface SecretRetriever {
    /**
     * Retrieves a secret with the given id from the jenkins master node
     *
     * @param id the secret id (as configured in the UI)
     * @return the decoded secret
     * @throws SecretNotFoundException occurs if the secret is not found
     */
    String retrieveSecret(String id) throws SecretNotFoundException;
}
