package io.jenkins.plugins.signpath;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import hudson.util.FormValidation;
import hudson.util.FormValidation.Kind;
import io.jenkins.plugins.signpath.TestUtils.SignPathJenkinsRule;
import org.junit.Rule;

public class SignPathPluginGlobalConfigurationTest {
    private SignPathPluginGlobalConfiguration config;

    @Rule
    public final SignPathJenkinsRule j = new SignPathJenkinsRule();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        config = new SignPathPluginGlobalConfiguration();
    }

    @Test
    public void testGetAndSetConnectorURL() {
        String url = "https://connector.example.com";
        config.setConnectorURL(url);
        assertEquals("The Connector URL should match the set value.", url, config.getConnectorURL());
    }

    @Test
    public void testDoCheckConnectorURL_Valid() {
        String validUrl = "https://connector.example.com";
        FormValidation result = config.doCheckConnectorURL(validUrl);
        assertEquals("Validation should pass with a valid url.", Kind.OK, result.kind);
    }

    @Test
    public void testDoCheckConnectorURL_Invalid() {
        String invalidUrl = "invalid-url";
        FormValidation result = config.doCheckConnectorURL(invalidUrl);
        assertEquals("Validation should fail.", FormValidation.Kind.ERROR, result.kind);
    }

    @Test
    public void testDoCheckConnectorURL_EmptyValue() {
        FormValidation result = config.doCheckConnectorURL("");
        assertEquals("Validation should not pass for an empty value.", FormValidation.error("Connector URL is required.").toString(), result.toString());
    }

    @Test
    public void testGetAndSetConnectorEndpointSlug() {
        String slug = "JenkinsOnPrem";
        config.setConnectorEndpointSlug(slug);
        assertEquals("The connector endpoint slug should match the set value.", slug, config.getConnectorEndpointSlug());
    }

    @Test
    public void testDoCheckConnectorEndpointSlug_Valid() {
        FormValidation result = config.doCheckConnectorEndpointSlug("JenkinsOnPrem");
        assertEquals("Validation should pass for a non-empty slug.", FormValidation.Kind.OK, result.kind);
    }

    @Test
    public void testDoCheckConnectorEndpointSlug_EmptyValue() {
        FormValidation result = config.doCheckConnectorEndpointSlug("");
        assertEquals("Validation should pass for an empty value (resolved at the step level).", FormValidation.Kind.OK, result.kind);
    }

    @Test
    public void testGetAndSetDefaultOrganizationId() {
        String organizationId = "123e4567-e89b-12d3-a456-426614174000";
        config.setOrganizationId(organizationId);
        assertEquals("The organization ID should match the set value.", organizationId, config.getOrganizationId());
    }

    @Test
    public void testDoCheckDefaultOrganizationId_ValidUUID() {
        String validUUID = "123e4567-e89b-12d3-a456-426614174000";
        FormValidation result = config.doCheckOrganizationId(validUUID);
        assertEquals("Validation should pass for a valid UUID.", FormValidation.Kind.OK, result.kind);
    }

    @Test
    public void testDoCheckDefaultOrganizationId_InvalidUUID() {
        String invalidUUID = "invalid-uuid";
        FormValidation result = config.doCheckOrganizationId(invalidUUID);
        assertEquals("Validation should fail for an invalid UUID.", FormValidation.error("Default organization ID must be a valid uuid.").toString(), result.toString());
    }

    @Test
    public void testDoCheckDefaultOrganizationId_EmptyValue() {
        FormValidation result = config.doCheckOrganizationId("");
        assertEquals("Validation should pass for an empty value.", FormValidation.Kind.OK, result.kind);
    }
}
