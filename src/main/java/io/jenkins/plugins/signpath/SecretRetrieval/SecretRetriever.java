package io.jenkins.plugins.signpath.SecretRetrieval;

import io.jenkins.plugins.signpath.Exceptions.SecretNotFoundException;

/**
 * Allows retrieving secrets
 */
public interface SecretRetriever {
    /**
     * Retrieves a secret with the given ID
     *
     * @param id the secret ID (as configured in the UI)
     * @throws SecretNotFoundException occurs if the secret is not found
     */
    String retrieveSecret(String id) throws SecretNotFoundException;
}
