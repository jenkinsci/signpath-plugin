package io.jenkins.plugins.signpath.SecretRetrieval;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import hudson.util.Secret;
import io.jenkins.plugins.signpath.Exceptions.SecretNotFoundException;
import io.jenkins.plugins.signpath.TestUtils.CredentialStoreUtils;
import io.jenkins.plugins.signpath.TestUtils.Some;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class CredentialBasedSecretRetrieverTest {

    private CredentialsStore credentialStore;
    private CredentialBasedSecretRetriever sut;

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Before
    public void setup() {
        Jenkins jenkins = Jenkins.getInstanceOrNull();
        sut = new CredentialBasedSecretRetriever(jenkins);
        credentialStore = CredentialStoreUtils.getCredentialStore(jenkins);
    }

    @Test
    public void retrieveSecret() throws IOException, SecretNotFoundException {
        String id = Some.stringNonEmpty();
        String secret = Some.stringNonEmpty();
        CredentialStoreUtils.addCredentials(credentialStore, CredentialsScope.SYSTEM, id, secret);

        // ACT
        String result = sut.retrieveSecret(id);

        // ASSERT
        assertEquals(secret, result);
    }

    @Test
    public void retrieveSecret_withDifferentDomain_works() throws IOException, SecretNotFoundException {
        String id = Some.stringNonEmpty();
        String secret = Some.stringNonEmpty();
        String domainName = Some.stringNonEmpty();
        credentialStore.addDomain(new Domain(domainName, Some.stringNonEmpty(), new ArrayList<>()));
        Domain domain = credentialStore.getDomainByName(domainName);
        assert domain != null;
        credentialStore.addCredentials(domain, new StringCredentialsImpl(CredentialsScope.SYSTEM, id, Some.stringNonEmpty(), Secret.fromString(secret)));

        // ACT
        String result = sut.retrieveSecret(id);

        // ASSERT
        assertEquals(secret, result);
    }

    @Test
    public void retrieveSecret_nonExisting_throws() {
        String nonExistingId = Some.stringNonEmpty();

        // ACT
        ThrowingRunnable act = () -> sut.retrieveSecret(nonExistingId);

        // ASSERT
        Throwable ex = assertThrows(SecretNotFoundException.class, act);
        assertEquals(ex.getMessage(), String.format("The secret '%s' could not be found in the credential store.", nonExistingId));
    }

    @Test
    public void retrieveSecret_wrongScope_throws() throws IOException {
        String id = Some.stringNonEmpty();
        String secret = Some.stringNonEmpty();
        CredentialStoreUtils.addCredentials(credentialStore, CredentialsScope.GLOBAL, id, secret);

        // ACT
        ThrowingRunnable act = () -> sut.retrieveSecret(id);

        // ASSERT
        Throwable ex = assertThrows(SecretNotFoundException.class, act);
        assertEquals(ex.getMessage(), String.format("The secret '%s' was configured with scope 'Global (Jenkins, nodes, items, all child items, etc)' but needs to be in scope 'System (Jenkins and nodes only)'.", id));
    }

    @Test
    public void retrieveSecret_nullScope_throws() throws IOException {
        String id = Some.stringNonEmpty();
        String secret = Some.stringNonEmpty();
        CredentialStoreUtils.addCredentials(credentialStore, null, id, secret);

        // ACT
        ThrowingRunnable act = () -> sut.retrieveSecret(id);

        // ASSERT
        Throwable ex = assertThrows(SecretNotFoundException.class, act);
        assertEquals(ex.getMessage(), String.format("The secret '%s' was configured with scope '<null>' but needs to be in scope 'System (Jenkins and nodes only)'.", id));
    }

    @Test
    public void retrieveSecret_wrongCredentialType_throws() throws IOException {
        String id = Some.stringNonEmpty();
        String secret = Some.stringNonEmpty();
        Domain domain = credentialStore.getDomains().get(0);
        credentialStore.addCredentials(domain, new UsernamePasswordCredentialsImpl(CredentialsScope.SYSTEM, id, Some.stringNonEmpty(), Some.stringNonEmpty(), secret));

        // ACT
        ThrowingRunnable act = () -> sut.retrieveSecret(id);

        // ASSERT
        Throwable ex = assertThrows(SecretNotFoundException.class, act);
        assertEquals(ex.getMessage(), String.format("The secret '%s' could not be found in the credential store.", id));
    }
}
