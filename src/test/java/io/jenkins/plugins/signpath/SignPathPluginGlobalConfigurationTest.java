package io.jenkins.plugins.signpath;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import hudson.util.FormValidation;
import io.jenkins.plugins.signpath.Exceptions.SecretNotFoundException;
import io.jenkins.plugins.signpath.SecretRetrieval.CredentialBasedSecretRetriever;
import io.jenkins.plugins.signpath.TestUtils.CredentialStoreUtils;
import io.jenkins.plugins.signpath.TestUtils.SignPathJenkinsRule;
import org.junit.Rule;

public class SignPathPluginGlobalConfigurationTest {
    private SignPathPluginGlobalConfiguration config;

    @Rule
    public final SignPathJenkinsRule j = new SignPathJenkinsRule();

    @Mock
    private CredentialBasedSecretRetriever secretRetrieverMock;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        config = new SignPathPluginGlobalConfiguration();
    }

    @Test
    public void testGetAndSetDefaultApiURL() {
        String url = "https://api.example.com";
        config.setDefaultApiURL(url);
        assertEquals("The default API URL should match the set value.", url, config.getDefaultApiURL());
    }

    @Test
    public void testGetAndSetDefaultTrustedBuildSystemCredentialId() {
        String credentialId = "test-credential-id";
        config.setDefaultTrustedBuildSystemCredentialId(credentialId);
        assertEquals("The TBS Credential ID should match the set value.", credentialId, config.getDefaultTrustedBuildSystemCredentialId());
    }
    
    @Test
    public void testDoCheckDefaultTrustedBuildSystemCredentialId_Valid() throws Exception {
        String validCredentialId = "valid-id";
        CredentialsStore credentialStore = CredentialStoreUtils.getCredentialStore(j.jenkins);
        assert credentialStore != null;
        CredentialStoreUtils.addCredentials(credentialStore, CredentialsScope.SYSTEM, validCredentialId, "dummySecret");
        FormValidation result = config.doCheckDefaultTrustedBuildSystemCredentialId(validCredentialId);
        assertEquals("Validation should pass with a valid credential ID.", FormValidation.ok(), result);
    }

    @Test
    public void testDoCheckDefaultTrustedBuildSystemCredentialId_Invalid() throws Exception {
        String invalidCredentialId = "invalid-id";
        doThrow(new SecretNotFoundException("Secret not found"))
                .when(secretRetrieverMock)
                .retrieveSecret(eq(invalidCredentialId), any());

        FormValidation result = config.doCheckDefaultTrustedBuildSystemCredentialId(invalidCredentialId);
        assertEquals("Validation should fail.", FormValidation.Kind.ERROR, result.kind);
    }

    @Test
    public void testGetAndSetDefaultOrganizationId() {
        String organizationId = "123e4567-e89b-12d3-a456-426614174000"; 
        config.setDefaultOrganizationId(organizationId);
        assertEquals("The default organization ID should match the set value.", organizationId, config.getDefaultOrganizationId());
    }

    @Test
    public void testDoCheckDefaultOrganizationId_ValidUUID() {
        String validUUID = "123e4567-e89b-12d3-a456-426614174000";
        FormValidation result = config.doCheckDefaultOrganizationId(validUUID);
        assertEquals("Validation should pass for a valid UUID.", FormValidation.ok(), result);
    }

    @Test
    public void testDoCheckDefaultOrganizationId_InvalidUUID() {
        String invalidUUID = "invalid-uuid";
        FormValidation result = config.doCheckDefaultOrganizationId(invalidUUID);
        assertEquals("Validation should fail for an invalid UUID.", FormValidation.error("Default Organization ID must be a valid uuid.").toString(), result.toString());
    }

    @Test
    public void testDoCheckDefaultOrganizationId_EmptyValue() {
        FormValidation result = config.doCheckDefaultOrganizationId("");
        assertEquals("Validation should pass for an empty value.", FormValidation.ok(), result);
    }
}