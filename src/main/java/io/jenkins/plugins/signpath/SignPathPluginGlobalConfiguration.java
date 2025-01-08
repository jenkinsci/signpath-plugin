package io.jenkins.plugins.signpath;

import com.cloudbees.plugins.credentials.CredentialsScope;
import hudson.Extension;
import jenkins.model.GlobalConfiguration;
import org.kohsuke.stapler.DataBoundSetter;
import hudson.util.FormValidation;
import io.jenkins.plugins.signpath.Common.PluginConstants;
import io.jenkins.plugins.signpath.Exceptions.SecretNotFoundException;
import io.jenkins.plugins.signpath.SecretRetrieval.CredentialBasedSecretRetriever;
import io.jenkins.plugins.signpath.SecretRetrieval.SecretRetriever;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.QueryParameter;

@Extension
public class SignPathPluginGlobalConfiguration extends GlobalConfiguration {

    private String apiURL = PluginConstants.DEFAULT_API_URL;
    private String defaultTrustedBuildSystemCredentialId = PluginConstants.DEFAULT_TBS_TOKEN_CREDENTIAL_ID;
    private String defaultOrganizationId;

    public SignPathPluginGlobalConfiguration() {
        load();
    }

    // ApiURL
    
    public String getApiURL() {
        return apiURL;
    }

    @DataBoundSetter
    public void setApiURL(String url) {
        this.apiURL = url;
        save();
    }

    public FormValidation doCheckApiURL(@QueryParameter String value) {
        if (value == null || value.trim().isEmpty()) {
            return FormValidation.error("Api URL is required.");
        }
        
        try {
            new URL(value);
            return FormValidation.ok();
        } catch (MalformedURLException e) {
            return FormValidation.error("Api URL must be a valid url.");
        }        
    }

    // DefaultTrustedBuildSystemCredential
    
    public String getDefaultTrustedBuildSystemCredentialId() {
        return defaultTrustedBuildSystemCredentialId;
    }

    @DataBoundSetter
    public void setDefaultTrustedBuildSystemCredentialId(String tbsCredentialId) {
        this.defaultTrustedBuildSystemCredentialId = tbsCredentialId;
        save();
    }
    
    public FormValidation doCheckDefaultTrustedBuildSystemCredentialId(@QueryParameter String value) {
        if (value == null || value.trim().isEmpty()) {
            return FormValidation.ok();
        }
        
        Jenkins jenkins = Jenkins.get();
        SecretRetriever secretRetriever = new CredentialBasedSecretRetriever(jenkins);
        
        try {
            // let's see if such secret exists
            // SYSTEM scope is required for TBS token
            secretRetriever.retrieveSecret(value, new CredentialsScope[] {CredentialsScope.SYSTEM});
            return FormValidation.ok();
        }
        catch(SecretNotFoundException ex)
        {
            return FormValidation.error(ex.getMessage());
        }
    }
    
    // DefaultOrganizationId

    public String getDefaultOrganizationId() {
        return defaultOrganizationId;
    }

    @DataBoundSetter
    public void setDefaultOrganizationId(String organizationId) {
        this.defaultOrganizationId = organizationId;
        save();
    }
    
    public FormValidation doCheckDefaultOrganizationId(@QueryParameter String value) {
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