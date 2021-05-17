package io.jenkins.plugins.signpath.SecretRetrieval;

import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import hudson.security.ACL;
import io.jenkins.plugins.signpath.Exceptions.SecretNotFoundException;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;

import java.util.Collections;
import java.util.List;

/**
 * Implementation of the
 * @see SecretRetriever
 * interface that uses the
 * @see CredentialsProvider
 * to retrieve secrets (this plugin is installed by default on Jenkins)
 * Only allows to retrieve secrets in SYSTEM scope as a security measure
 * (those cannot be retrieve from agent nodes / build scripts)
 */
public class CredentialBasedSecretRetriever implements SecretRetriever {
    private final Jenkins jenkins;

    public CredentialBasedSecretRetriever(Jenkins jenkins) {
        this.jenkins = jenkins;
    }

    @Override
    public String retrieveSecret(String id) throws SecretNotFoundException {
        List<StringCredentials> credentials =
                CredentialsProvider.lookupCredentials(StringCredentials.class, jenkins, ACL.SYSTEM, Collections.emptyList());
        CredentialsMatcher matcher = CredentialsMatchers.withId(id);
        StringCredentials credential = CredentialsMatchers.firstOrNull(credentials, matcher);

        if (credential == null) {
            throw new SecretNotFoundException(String.format("The secret '%s' could not be found in the credential store.", id));
        }

        if (credential.getScope() != CredentialsScope.SYSTEM) {
            CredentialsScope scope = credential.getScope();
            String scopeName = scope == null ? "<null>" : scope.getDisplayName();
            throw new SecretNotFoundException(
                    String.format("The secret '%s' was configured with scope '%s' but needs to be in scope '%s'.",
                            id, scopeName, CredentialsScope.SYSTEM.getDisplayName()));
        }

        return credential.getSecret().getPlainText();
    }
}
