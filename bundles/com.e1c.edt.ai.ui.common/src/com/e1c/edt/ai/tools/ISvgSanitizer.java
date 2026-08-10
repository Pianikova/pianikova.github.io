/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

/**
 * Validates and sanitizes SVG markup authored by the model: repairs common, unambiguous mistakes
 * (missing namespace, wrapping markdown fence, stray DOCTYPE), removes unsafe constructs (scripts,
 * event handlers, external references), and reports well-formedness errors with a line and column
 * so the model can self-correct.
 */
public interface ISvgSanitizer
{
    /**
     * Parses, repairs and sanitizes SVG markup. Never throws for malformed or unsafe input - the
     * outcome, including well-formedness errors with a line and column, is reported in the result.
     *
     * @param source SVG markup authored by the model or read from disk; may be {@code null} or blank
     * @return the sanitize result, never {@code null}
     */
    SvgSanitizeResult sanitize(String source);
}
