package io.jenkins.plugins.SignPath.SecretRetrieval;

import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import hudson.security.ACL;
import io.jenkins.plugins.SignPath.Exceptions.SecretNotFoundException;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;

import java.util.Collections;
import java.util.List;

public class CredentialBasedSecretRetriever {
    private Jenkins jenkins;

    public CredentialBasedSecretRetriever(Jenkins jenkins){
        this.jenkins = jenkins;
    }

    public String retrieveSecret(String id) throws SecretNotFoundException {
        List<StringCredentials> credentials =
                CredentialsProvider.lookupCredentials(StringCredentials.class, jenkins, ACL.SYSTEM, Collections.emptyList());
        CredentialsMatcher matcher = CredentialsMatchers.withId(id);
        StringCredentials credential = CredentialsMatchers.firstOrNull(credentials, matcher);

        if(credential == null) {
            throw new SecretNotFoundException(String.format("The secret '%s' could not be found in the credential store.", id));
        }

        if(credential.getScope() != CredentialsScope.SYSTEM) {
            throw new SecretNotFoundException(
                    String.format("The secret '%s' was configured with scope '%s' but needs to be in '%s' scope.",
                            id, credential.getScope().getDisplayName(), CredentialsScope.SYSTEM.getDisplayName()));
        }

        return credential.getSecret().getPlainText();
    }
}
