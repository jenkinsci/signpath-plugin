package io.jenkins.plugins.signpath.PipelineData;

import io.jenkins.plugins.signpath.PipelineData.Model.PipelineDataDto;

import java.util.Collections;
import java.util.List;

/**
 * SIGN-8500 PoC. Wraps the assembled PipelineDataDto with a list of human-readable
 * reasons for any sub-object that was deliberately left unset (refuse-to-guess).
 *
 * The reasons are intended for log output so an operator running the spike can tell
 * whether their pipeline shape is supported, and if not, why.
 */
public final class PipelineDataExtractionResult {
    private final PipelineDataDto data;
    private final List<String> skipReasons;

    public PipelineDataExtractionResult(PipelineDataDto data, List<String> skipReasons) {
        this.data = data;
        this.skipReasons = Collections.unmodifiableList(skipReasons);
    }

    public PipelineDataDto getData() {
        return data;
    }

    public List<String> getSkipReasons() {
        return skipReasons;
    }
}
