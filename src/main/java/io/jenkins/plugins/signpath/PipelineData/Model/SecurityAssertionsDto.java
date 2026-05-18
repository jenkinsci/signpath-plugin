package io.jenkins.plugins.signpath.PipelineData.Model;

/**
 * SLSA security assertions reported with the build. SIGN-8581 hard-codes every
 * field to {@code false}; deferred to a follow-up story to compute real values.
 */
public final class SecurityAssertionsDto {
    private final boolean ephemeral;
    private final boolean noAccessToPlatformSecrets;
    private final boolean noConcurrentJobsOnAgent;
    private final boolean noImplicitCaching;
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
