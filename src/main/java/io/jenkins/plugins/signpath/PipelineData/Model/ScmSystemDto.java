package io.jenkins.plugins.signpath.PipelineData.Model;

/**
 * SCM system identification. Reserved for a follow-up story; SIGN-8581 always
 * leaves this field null.
 */
public final class ScmSystemDto {
    private final String id;
    private final String webUrl;

    public ScmSystemDto(String id, String webUrl) {
        this.id = id;
        this.webUrl = webUrl;
    }

    public String getId() {
        return id;
    }

    public String getWebUrl() {
        return webUrl;
    }
}
