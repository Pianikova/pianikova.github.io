/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.util.Collections;
import java.util.List;

/**
 * Outcome of {@link ISvgSanitizer#sanitize(String)}. Never represents a thrown exception - any
 * failure to parse or validate the markup is reported as an {@code ERROR} diagnostic with
 * {@link #isValid()} {@code false}.
 */
public final class SvgSanitizeResult
{
    private final boolean valid;
    private final boolean modified;
    private final String sanitizedSource;
    private final List<SvgDiagnostic> diagnostics;
    private final SvgMetadata metadata;

    public SvgSanitizeResult(boolean valid, boolean modified, String sanitizedSource,
        List<SvgDiagnostic> diagnostics, SvgMetadata metadata)
    {
        this.valid = valid;
        this.modified = modified;
        this.sanitizedSource = sanitizedSource;
        this.diagnostics = Collections.unmodifiableList(diagnostics);
        this.metadata = metadata;
    }

    /**
     * @return {@code true} when the markup is well-formed SVG and contains no diagnostic of
     *         {@link SvgDiagnostic.Severity#ERROR}.
     */
    public boolean isValid()
    {
        return valid;
    }

    /**
     * @return {@code true} when the sanitizer changed the markup (repair or removal), regardless
     *         of {@link #isValid()}.
     */
    public boolean isModified()
    {
        return modified;
    }

    /**
     * @return the sanitized, serialized SVG markup, ready to write to disk or embed as a data URI;
     *         {@code null} when {@link #isValid()} is {@code false}.
     */
    public String getSanitizedSource()
    {
        return sanitizedSource;
    }

    /**
     * @return diagnostics in the order they were found, errors and warnings intermixed; never null.
     */
    public List<SvgDiagnostic> getDiagnostics()
    {
        return diagnostics;
    }

    /**
     * @return structural metadata of the sanitized document; {@code null} when {@link #isValid()}
     *         is {@code false}.
     */
    public SvgMetadata getMetadata()
    {
        return metadata;
    }
}
