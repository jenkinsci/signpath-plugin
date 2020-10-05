package io.jenkins.plugins.SignPath.TestUtils;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;

import java.io.IOException;

public class CredentialStoreUtils {
    public static void addCredentials(CredentialsStore credentialsStore, CredentialsScope scope, String id, String secret) throws IOException {
        Domain domain = credentialsStore.getDomains().get(0);
        credentialsStore.addCredentials(domain,
                new StringCredentialsImpl(scope, id, Some.stringNonEmpty(), Secret.fromString(secret)));
    }

    public static CredentialsStore getCredentialStore(Jenkins jenkins) {
        for (CredentialsStore credentialsStore : CredentialsProvider.lookupStores(jenkins)) {
            if(SystemCredentialsProvider.StoreImpl.class.isAssignableFrom(credentialsStore.getClass())){
                return credentialsStore;
            }
        }

        return null;
    }
}
