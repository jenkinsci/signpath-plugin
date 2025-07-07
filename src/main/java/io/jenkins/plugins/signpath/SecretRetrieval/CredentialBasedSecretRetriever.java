package io.jenkins.plugins.signpath.SecretRetrieval;

import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import hudson.security.ACL;
import hudson.util.Secret;
import io.jenkins.plugins.signpath.Exceptions.SecretNotFoundException;
import java.util.Arrays;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
    public Secret retrieveSecret(String id) throws SecretNotFoundException {
        CredentialsScope[] allowedScopes = { CredentialsScope.SYSTEM };
        return retrieveSecret(id, allowedScopes);
    }

    @Override
    public Secret retrieveSecret(String id, CredentialsScope[] allowedScopes) throws SecretNotFoundException {
        List<StringCredentials> credentials =
                CredentialsProvider.lookupCredentialsInItemGroup(StringCredentials.class, jenkins, ACL.SYSTEM2, Collections.emptyList());
        CredentialsMatcher matcher = CredentialsMatchers.withId(id);
        StringCredentials credential = CredentialsMatchers.firstOrNull(credentials, matcher);

        if (credential == null) {
            throw new SecretNotFoundException(String.format("The secret '%s' could not be found in the credential store.", id));
        }

        CredentialsScope credentialScope = credential.getScope();

        if (allowedScopes.length > 0 && !Arrays.asList(allowedScopes).contains(credentialScope)) {
            String scopeName = credentialScope == null ? "<null>" : credentialScope.getDisplayName();
            String allowedScopesStr = Arrays.stream(allowedScopes)
                      .map(CredentialsScope::getDisplayName)
                      .collect(Collectors.joining("' or '"));

            throw new SecretNotFoundException(
                    String.format("The secret '%s' was configured with scope '%s' but needs to be in scope(s) '%s'.",
                            id, scopeName, allowedScopesStr));
        }

        return credential.getSecret();
    }
}
