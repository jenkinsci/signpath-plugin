package io.jenkins.plugins.signpath.SecretRetrieval;

import hudson.util.Secret;
import io.jenkins.plugins.signpath.Exceptions.SecretNotFoundException;

/**
 * Allows retrieving secrets
 */
public interface SecretRetriever {
    /**
     * Retrieves a secret with the given ID
     *
     * @param id the secret ID (as configured in the UI)
     * @return The secret
     * @throws SecretNotFoundException occurs if the secret is not found
     */
    Secret retrieveSecret(String id) throws SecretNotFoundException;
}
