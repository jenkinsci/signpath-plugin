package io.jenkins.plugins.signpath;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import hudson.util.FormValidation;
import hudson.util.FormValidation.Kind;
import io.jenkins.plugins.signpath.Exceptions.SecretNotFoundException;
import io.jenkins.plugins.signpath.SecretRetrieval.CredentialBasedSecretRetriever;
import io.jenkins.plugins.signpath.TestUtils.CredentialStoreUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;

@WithJenkins
@ExtendWith(MockitoExtension.class)
class SignPathPluginGlobalConfigurationTest {
    private SignPathPluginGlobalConfiguration config;

    private JenkinsRule j;

    @Mock(strictness = Mock.Strictness.LENIENT)
    private CredentialBasedSecretRetriever secretRetrieverMock;

    @BeforeEach
    void setUp(JenkinsRule j) {
        this.j = j;
        config = new SignPathPluginGlobalConfiguration();
    }

    @Test
    void testGetAndSetApiURL() {
        String url = "https://api.example.com";
        config.setApiURL(url);
        assertEquals(url, config.getApiURL(), "The API URL should match the set value.");
    }

    @Test
    void testDoCheckApiURL_Valid() {
        String validUrl = "https://api.example.com";
        FormValidation result = config.doCheckApiURL(validUrl);
        assertEquals(Kind.OK, result.kind, "Validation should pass with a valid url.");
    }

    @Test
    void testDoCheckApiURL_Invalid() {
        String invalidUrl = "invalid-url";
        FormValidation result = config.doCheckApiURL(invalidUrl);
        assertEquals(FormValidation.Kind.ERROR, result.kind, "Validation should fail.");
    }

    @Test
    void testDoCheckApiURL_EmptyValue() {
        FormValidation result = config.doCheckApiURL("");
        assertEquals(FormValidation.error("Api URL is required.").toString(), result.toString(), "Validation should not pass for an empty value.");
    }

    @Test
    void testGetAndSetDefaultTrustedBuildSystemCredentialId() {
        String credentialId = "test-credential-id";
        config.setTrustedBuildSystemCredentialId(credentialId);
        assertEquals(credentialId, config.getTrustedBuildSystemCredentialId(), "The TBS Credential ID should match the set value.");
    }

    @Test
    void testDoCheckDefaultTrustedBuildSystemCredentialId_Valid() throws Exception {
        String validCredentialId = "valid-id";
        CredentialsStore credentialStore = CredentialStoreUtils.getCredentialStore(j.jenkins);
        assert credentialStore != null;
        CredentialStoreUtils.addCredentials(credentialStore, CredentialsScope.SYSTEM, validCredentialId, "dummySecret");
        FormValidation result = config.doCheckTrustedBuildSystemCredentialId(validCredentialId);
        assertEquals(FormValidation.Kind.OK, result.kind, "Validation should pass with a valid credential ID.");
    }

    @Test
    void testDoCheckDefaultTrustedBuildSystemCredentialId_Invalid() throws Exception {
        String invalidCredentialId = "invalid-id";
        doThrow(new SecretNotFoundException("Secret not found"))
                .when(secretRetrieverMock)
                .retrieveSecret(eq(invalidCredentialId), any());

        FormValidation result = config.doCheckTrustedBuildSystemCredentialId(invalidCredentialId);
        assertEquals(FormValidation.Kind.ERROR, result.kind, "Validation should fail.");
    }

    @Test
    void testDoCheckDefaultTrustedBuildSystemCredentialId_EmptyValue() {
        FormValidation result = config.doCheckTrustedBuildSystemCredentialId("");
        assertEquals(FormValidation.Kind.OK, result.kind, "Validation should pass for an empty value.");
    }

    @Test
    void testGetAndSetDefaultOrganizationId() {
        String organizationId = "123e4567-e89b-12d3-a456-426614174000";
        config.setOrganizationId(organizationId);
        assertEquals(organizationId, config.getOrganizationId(), "The organization ID should match the set value.");
    }

    @Test
    void testDoCheckDefaultOrganizationId_ValidUUID() {
        String validUUID = "123e4567-e89b-12d3-a456-426614174000";
        FormValidation result = config.doCheckOrganizationId(validUUID);
        assertEquals(FormValidation.Kind.OK, result.kind, "Validation should pass for a valid UUID.");
    }

    @Test
    void testDoCheckDefaultOrganizationId_InvalidUUID() {
        String invalidUUID = "invalid-uuid";
        FormValidation result = config.doCheckOrganizationId(invalidUUID);
        assertEquals(FormValidation.error("Default organization ID must be a valid uuid.").toString(), result.toString(), "Validation should fail for an invalid UUID.");
    }

    @Test
    void testDoCheckDefaultOrganizationId_EmptyValue() {
        FormValidation result = config.doCheckOrganizationId("");
        assertEquals(FormValidation.Kind.OK, result.kind, "Validation should pass for an empty value.");
    }
}
