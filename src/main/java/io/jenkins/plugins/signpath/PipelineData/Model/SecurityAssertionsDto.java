package io.jenkins.plugins.signpath.PipelineData.Model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * SIGN-8500. Mirror of SignPath's PipelineDataSecurityAssertionsDto.
 *
 * <p>The five SLSA 1.0 boolean assertions per the C# contract. All hardcoded to
 * {@code false} per the ticket boundaries (we do not assert anything in v1).</p>
 */
@JsonPropertyOrder({"Ephemeral", "NoAccessToPlatformSecrets", "NoConcurrentJobsOnAgent",
        "NoImplicitCaching", "NoImplicitRemoteAccessToAgent"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class SecurityAssertionsDto {
    @JsonProperty("Ephemeral")
    private final boolean ephemeral;
    @JsonProperty("NoAccessToPlatformSecrets")
    private final boolean noAccessToPlatformSecrets;
    @JsonProperty("NoConcurrentJobsOnAgent")
    private final boolean noConcurrentJobsOnAgent;
    @JsonProperty("NoImplicitCaching")
    private final boolean noImplicitCaching;
    @JsonProperty("NoImplicitRemoteAccessToAgent")
    private final boolean noImplicitRemoteAccessToAgent;

    public SecurityAssertionsDto() {
        this(false, false, false, false, false);
    }

    public SecurityAssertionsDto(boolean ephemeral,
                                 boolean noAccessToPlatformSecrets,
                                 boolean noConcurrentJobsOnAgent,
                                 boolean noImplicitCaching,
                                 boolean noImplicitRemoteAccessToAgent) {
        this.ephemeral = ephemeral;
        this.noAccessToPlatformSecrets = noAccessToPlatformSecrets;
        this.noConcurrentJobsOnAgent = noConcurrentJobsOnAgent;
        this.noImplicitCaching = noImplicitCaching;
        this.noImplicitRemoteAccessToAgent = noImplicitRemoteAccessToAgent;
    }

    public boolean isEphemeral() {
        return ephemeral;
    }

    public boolean isNoAccessToPlatformSecrets() {
        return noAccessToPlatformSecrets;
    }

    public boolean isNoConcurrentJobsOnAgent() {
        return noConcurrentJobsOnAgent;
    }

    public boolean isNoImplicitCaching() {
        return noImplicitCaching;
    }

    public boolean isNoImplicitRemoteAccessToAgent() {
        return noImplicitRemoteAccessToAgent;
    }
}
