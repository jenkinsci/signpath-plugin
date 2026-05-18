package io.jenkins.plugins.signpath.PipelineData;

import io.jenkins.plugins.signpath.PipelineData.Model.BuildDto;
import io.jenkins.plugins.signpath.PipelineData.Model.CommitDto;
import io.jenkins.plugins.signpath.PipelineData.Model.DefinitionDto;
import io.jenkins.plugins.signpath.PipelineData.Model.OriginDto;
import io.jenkins.plugins.signpath.PipelineData.Model.PipelineDataDto;
import io.jenkins.plugins.signpath.PipelineData.Model.ScmSystemDto;
import io.jenkins.plugins.signpath.PipelineData.Model.SecurityAssertionsDto;
import io.jenkins.plugins.signpath.PipelineData.Model.SourceCodeDto;

/**
 * Hand-written serializer for the SignPath PipelineData JSON contract. The
 * schema is small and pinned, so we avoid pulling in Jackson just to write a
 * dozen fields.
 *
 * <p>Property names mirror the C# {@code PipelineDataDto} (PascalCase) and the
 * top-level schema version uses the {@code _version} JSON name.
 */
public final class PipelineDataJsonSerializer {

    private PipelineDataJsonSerializer() {
    }

    public static String toJson(PipelineDataDto dto) {
        StringBuilder sb = new StringBuilder();
        writeRoot(sb, dto);
        return sb.toString();
    }

    private static void writeRoot(StringBuilder sb, PipelineDataDto dto) {
        sb.append('{');
        boolean first = true;
        first = writeString(sb, first, "_version", dto.getVersion());
        first = writeBuild(sb, first, dto.getBuild());
        writeSourceCode(sb, first, dto.getSourceCode());
        sb.append('}');
    }

    private static boolean writeBuild(StringBuilder sb, boolean first, BuildDto build) {
        if (build == null) {
            return first;
        }
        comma(sb, first);
        appendKey(sb, "Build");
        sb.append('{');
        boolean firstInner = true;
        firstInner = writeDefinition(sb, firstInner, build.getDefinition());
        firstInner = writeSecurityAssertions(sb, firstInner, build.getSecurityAssertions());
        firstInner = writeString(sb, firstInner, "StartedAt", build.getStartedAt());
        if (build.getSystem() != null) {
            comma(sb, firstInner);
            appendKey(sb, "System");
            sb.append('{');
            writeString(sb, true, "Id", build.getSystem().getId());
            sb.append('}');
            firstInner = false;
        }
        firstInner = writeString(sb, firstInner, "WebUrl", build.getWebUrl());
        sb.append('}');
        return false;
    }

    private static boolean writeDefinition(StringBuilder sb, boolean first, DefinitionDto def) {
        if (def == null) {
            return first;
        }
        comma(sb, first);
        appendKey(sb, "Definition");
        sb.append('{');
        boolean firstInner = true;
        firstInner = writeString(sb, firstInner, "Repository", def.getRepository());
        firstInner = writeString(sb, firstInner, "Branch", def.getBranch());
        firstInner = writeString(sb, firstInner, "Path", def.getPath());
        firstInner = writeCommit(sb, firstInner, def.getCommit());
        writeString(sb, firstInner, "WebUrl", def.getWebUrl());
        sb.append('}');
        return false;
    }

    private static boolean writeCommit(StringBuilder sb, boolean first, CommitDto commit) {
        if (commit == null) {
            return first;
        }
        comma(sb, first);
        appendKey(sb, "Commit");
        sb.append('{');
        boolean firstInner = true;
        firstInner = writeString(sb, firstInner, "Id", commit.getId());
        writeString(sb, firstInner, "WebUrl", commit.getWebUrl());
        sb.append('}');
        return false;
    }

    private static boolean writeSecurityAssertions(StringBuilder sb, boolean first, SecurityAssertionsDto sa) {
        if (sa == null) {
            return first;
        }
        comma(sb, first);
        appendKey(sb, "SecurityAssertions");
        sb.append('{');
        boolean firstInner = true;
        firstInner = writeBool(sb, firstInner, "Ephemeral", sa.isEphemeral());
        firstInner = writeBool(sb, firstInner, "NoAccessToPlatformSecrets", sa.isNoAccessToPlatformSecrets());
        firstInner = writeBool(sb, firstInner, "NoConcurrentJobsOnAgent", sa.isNoConcurrentJobsOnAgent());
        firstInner = writeBool(sb, firstInner, "NoImplicitCaching", sa.isNoImplicitCaching());
        writeBool(sb, firstInner, "NoImplicitRemoteAccessToAgent", sa.isNoImplicitRemoteAccessToAgent());
        sb.append('}');
        return false;
    }

    private static void writeSourceCode(StringBuilder sb, boolean first, SourceCodeDto sc) {
        if (sc == null) {
            return;
        }
        comma(sb, first);
        appendKey(sb, "SourceCode");
        sb.append('{');
        boolean firstInner = true;
        if (sc.getIsPublicRepository() != null) {
            comma(sb, firstInner);
            appendKey(sb, "IsPublicRepository");
            sb.append(sc.getIsPublicRepository().booleanValue() ? "true" : "false");
            firstInner = false;
        }
        firstInner = writeOrigin(sb, firstInner, sc.getOrigin());
        writeScmSystem(sb, firstInner, sc.getScmSystem());
        sb.append('}');
    }

    private static boolean writeOrigin(StringBuilder sb, boolean first, OriginDto origin) {
        if (origin == null) {
            return first;
        }
        comma(sb, first);
        appendKey(sb, "Origin");
        sb.append('{');
        boolean firstInner = true;
        firstInner = writeString(sb, firstInner, "Type", origin.getType());
        firstInner = writeString(sb, firstInner, "Url", origin.getUrl());
        firstInner = writeString(sb, firstInner, "Branch", origin.getBranch());
        firstInner = writeCommit(sb, firstInner, origin.getCommit());
        writeString(sb, firstInner, "WebUrl", origin.getWebUrl());
        sb.append('}');
        return false;
    }

    private static void writeScmSystem(StringBuilder sb, boolean first, ScmSystemDto scm) {
        if (scm == null) {
            return;
        }
        comma(sb, first);
        appendKey(sb, "ScmSystem");
        sb.append('{');
        boolean firstInner = true;
        firstInner = writeString(sb, firstInner, "Id", scm.getId());
        writeString(sb, firstInner, "WebUrl", scm.getWebUrl());
        sb.append('}');
    }

    private static boolean writeString(StringBuilder sb, boolean first, String name, String value) {
        if (value == null) {
            return first;
        }
        comma(sb, first);
        appendKey(sb, name);
        appendString(sb, value);
        return false;
    }

    private static boolean writeBool(StringBuilder sb, boolean first, String name, boolean value) {
        comma(sb, first);
        appendKey(sb, name);
        sb.append(value ? "true" : "false");
        return false;
    }

    private static void comma(StringBuilder sb, boolean first) {
        if (!first) {
            sb.append(',');
        }
    }

    private static void appendKey(StringBuilder sb, String name) {
        appendString(sb, name);
        sb.append(':');
    }

    private static void appendString(StringBuilder sb, String value) {
        sb.append('"');
        int len = value.length();
        for (int i = 0; i < len; i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                    break;
            }
        }
        sb.append('"');
    }
}
