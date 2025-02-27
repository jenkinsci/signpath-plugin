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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.io.IOException;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@WithJenkins
class CredentialBasedSecretRetrieverTest {

    private CredentialsStore credentialStore;
    private CredentialBasedSecretRetriever sut;

    @BeforeEach
    void setUp(JenkinsRule j) {
        Jenkins jenkins = Jenkins.getInstanceOrNull();
        sut = new CredentialBasedSecretRetriever(jenkins);
        credentialStore = CredentialStoreUtils.getCredentialStore(jenkins);
    }

    @Test
    void retrieveSecret() throws IOException, SecretNotFoundException {
        String id = Some.stringNonEmpty();
        String secret = Some.stringNonEmpty();
        CredentialStoreUtils.addCredentials(credentialStore, CredentialsScope.SYSTEM, id, secret);

        // ACT
        Secret result = sut.retrieveSecret(id);

        // ASSERT
        assertEquals(secret, result.getPlainText());
    }

    @Test
    void retrieveSecretNoScopeRestriction() throws IOException, SecretNotFoundException {
        String id = Some.stringNonEmpty();
        String secret = Some.stringNonEmpty();
        CredentialStoreUtils.addCredentials(credentialStore, CredentialsScope.USER, id, secret);

        // ACT
        CredentialsScope[] emptyScopesSet  = { };
        Secret result = sut.retrieveSecret(id, emptyScopesSet);

        // ASSERT
        assertEquals(secret, result.getPlainText());
    }

    @Test
    void retrieveSecret_withDifferentDomain_works() throws IOException, SecretNotFoundException {
        String id = Some.stringNonEmpty();
        String secret = Some.stringNonEmpty();
        String domainName = Some.stringNonEmpty();
        credentialStore.addDomain(new Domain(domainName, Some.stringNonEmpty(), new ArrayList<>()));
        Domain domain = credentialStore.getDomainByName(domainName);
        assert domain != null;
        credentialStore.addCredentials(domain, new StringCredentialsImpl(CredentialsScope.SYSTEM, id, Some.stringNonEmpty(), Secret.fromString(secret)));

        // ACT
        Secret result = sut.retrieveSecret(id);

        // ASSERT
        assertEquals(secret, result.getPlainText());
    }

    @Test
    void retrieveSecret_nonExisting_throws() {
        String nonExistingId = Some.stringNonEmpty();

        // ACT
        Executable act = () -> sut.retrieveSecret(nonExistingId);

        // ASSERT
        Throwable ex = assertThrows(SecretNotFoundException.class, act);
        assertEquals(ex.getMessage(), String.format("The secret '%s' could not be found in the credential store.", nonExistingId));
    }

    @Test
    void retrieveSecret_wrongScope_throws() throws IOException {
        String id = Some.stringNonEmpty();
        String secret = Some.stringNonEmpty();
        CredentialStoreUtils.addCredentials(credentialStore, CredentialsScope.GLOBAL, id, secret);

        // ACT
        Executable act = () -> sut.retrieveSecret(id);

        // ASSERT
        Throwable ex = assertThrows(SecretNotFoundException.class, act);
        assertEquals(ex.getMessage(), String.format("The secret '%s' was configured with scope 'Global (Jenkins, nodes, items, all child items, etc)' but needs to be in scope(s) 'System (Jenkins and nodes only)'.", id));
    }

    @Test
    void retrieveSecret_wrongScope_multiple_scopes_throws() throws IOException {
        String id = Some.stringNonEmpty();
        String secret = Some.stringNonEmpty();
        CredentialStoreUtils.addCredentials(credentialStore, CredentialsScope.GLOBAL, id, secret);

        // ACT
        Executable act = () -> sut.retrieveSecret(id, new CredentialsScope[] { CredentialsScope.SYSTEM, CredentialsScope.USER, });

        // ASSERT
        Throwable ex = assertThrows(SecretNotFoundException.class, act);
        assertEquals(ex.getMessage(), String.format("The secret '%s' was configured with scope 'Global (Jenkins, nodes, items, all child items, etc)' but needs to be in scope(s) 'System (Jenkins and nodes only)' or 'User'.", id));
    }

    @Test
    void retrieveSecret_nullScope_throws() throws IOException {
        String id = Some.stringNonEmpty();
        String secret = Some.stringNonEmpty();
        CredentialStoreUtils.addCredentials(credentialStore, null, id, secret);

        // ACT
        Executable act = () -> sut.retrieveSecret(id);

        // ASSERT
        Throwable ex = assertThrows(SecretNotFoundException.class, act);
        assertEquals(ex.getMessage(), String.format("The secret '%s' was configured with scope '<null>' but needs to be in scope(s) 'System (Jenkins and nodes only)'.", id));
    }

    @Test
    void retrieveSecret_wrongCredentialType_throws() throws IOException {
        String id = Some.stringNonEmpty();
        String secret = Some.stringNonEmpty();
        Domain domain = credentialStore.getDomains().get(0);
        credentialStore.addCredentials(domain, new UsernamePasswordCredentialsImpl(CredentialsScope.SYSTEM, id, Some.stringNonEmpty(), Some.stringNonEmpty(), secret));

        // ACT
        Executable act = () -> sut.retrieveSecret(id);

        // ASSERT
        Throwable ex = assertThrows(SecretNotFoundException.class, act);
        assertEquals(ex.getMessage(), String.format("The secret '%s' could not be found in the credential store.", id));
    }
}
