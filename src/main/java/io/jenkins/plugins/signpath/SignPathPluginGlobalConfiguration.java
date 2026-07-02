package io.jenkins.plugins.signpath;

import hudson.Extension;
import jenkins.model.GlobalConfiguration;
import org.kohsuke.stapler.DataBoundSetter;
import hudson.util.FormValidation;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;
import org.kohsuke.stapler.QueryParameter;

@Extension
public class SignPathPluginGlobalConfiguration extends GlobalConfiguration {

    // URL of the SignPath Pipeline Connector (the plugin no longer talks to the SignPath Application directly).
    private String connectorURL;

    // the fields below are default values which might be overridden at the pipeline level
    private String connectorEndpointSlug;
    private String organizationId;

    public SignPathPluginGlobalConfiguration() {
        load();
    }

    // ConnectorURL

    public String getConnectorURL() {
        return connectorURL;
    }

    @DataBoundSetter
    public void setConnectorURL(String url) {
        this.connectorURL = url;
        save();
    }

    public FormValidation doCheckConnectorURL(@QueryParameter String value) {
        if (value == null || value.trim().isEmpty()) {
            return FormValidation.error("Connector URL is required.");
        }

        try {
            new URL(value);
            return FormValidation.ok();
        } catch (MalformedURLException e) {
            return FormValidation.error("Connector URL must be a valid url.");
        }
    }

    // ConnectorEndpointSlug

    public String getConnectorEndpointSlug() {
        return connectorEndpointSlug;
    }

    @DataBoundSetter
    public void setConnectorEndpointSlug(String connectorEndpointSlug) {
        this.connectorEndpointSlug = connectorEndpointSlug;
        save();
    }

    public FormValidation doCheckConnectorEndpointSlug(@QueryParameter String value) {
        if (value == null || value.trim().isEmpty()) {
            return FormValidation.ok();
        }

        return FormValidation.ok();
    }

    // OrganizationId

    public String getOrganizationId() {
        return organizationId;
    }

    @DataBoundSetter
    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
        save();
    }

    public FormValidation doCheckOrganizationId(@QueryParameter String value) {
        if (value == null || value.trim().isEmpty()) {
            return FormValidation.ok();
        }

        if (!isValidUUID(value)) {
            return FormValidation.error("Default organization ID must be a valid uuid.");
        }

        return FormValidation.ok();
    }

    protected boolean isValidUUID(String input) {
        try {
            UUID.fromString(input);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
