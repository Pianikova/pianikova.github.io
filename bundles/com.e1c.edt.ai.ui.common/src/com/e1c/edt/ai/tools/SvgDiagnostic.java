/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

/**
 * A single well-formedness error or sanitization warning produced while processing SVG markup.
 * Line and column are 1-based; {@code 0} means "unknown" (for example, a diagnostic that is not
 * tied to a specific source position).
 */
public final class SvgDiagnostic
{
    /**
     * Severity of a {@link SvgDiagnostic}.
     */
    public enum Severity
    {
        ERROR, WARNING
    }

    // Hard errors - the markup could not be sanitized and nothing is written.
    public static final String PARSE_ERROR = "PARSE_ERROR"; //$NON-NLS-1$
    public static final String NOT_SVG_ROOT = "NOT_SVG_ROOT"; //$NON-NLS-1$
    public static final String EMPTY_SOURCE = "EMPTY_SOURCE"; //$NON-NLS-1$
    public static final String TOO_LARGE = "TOO_LARGE"; //$NON-NLS-1$
    public static final String TOO_MANY_ELEMENTS = "TOO_MANY_ELEMENTS"; //$NON-NLS-1$

    // Automatic repairs and removals - reported as warnings, markup is still saved.
    public static final String CODE_FENCE_REMOVED = "CODE_FENCE_REMOVED"; //$NON-NLS-1$
    public static final String DOCTYPE_REMOVED = "DOCTYPE_REMOVED"; //$NON-NLS-1$
    public static final String MISSING_XMLNS_FIXED = "MISSING_XMLNS_FIXED"; //$NON-NLS-1$
    public static final String XLINK_NS_ADDED = "XLINK_NS_ADDED"; //$NON-NLS-1$
    public static final String SCRIPT_REMOVED = "SCRIPT_REMOVED"; //$NON-NLS-1$
    public static final String EVENT_ATTRIBUTE_REMOVED = "EVENT_ATTRIBUTE_REMOVED"; //$NON-NLS-1$
    public static final String FOREIGN_OBJECT_REMOVED = "FOREIGN_OBJECT_REMOVED"; //$NON-NLS-1$
    public static final String EXTERNAL_REFERENCE_REMOVED = "EXTERNAL_REFERENCE_REMOVED"; //$NON-NLS-1$
    public static final String UNSAFE_URL_REMOVED = "UNSAFE_URL_REMOVED"; //$NON-NLS-1$
    public static final String STYLE_REMOVED = "STYLE_REMOVED"; //$NON-NLS-1$
    public static final String FOREIGN_NAMESPACE_REMOVED = "FOREIGN_NAMESPACE_REMOVED"; //$NON-NLS-1$
    public static final String ANIMATION_REMOVED = "ANIMATION_REMOVED"; //$NON-NLS-1$
    public static final String PROCESSING_INSTRUCTION_REMOVED = "PROCESSING_INSTRUCTION_REMOVED"; //$NON-NLS-1$
    public static final String NO_VIEWBOX = "NO_VIEWBOX"; //$NON-NLS-1$

    private final Severity severity;
    private final String code;
    private final String message;
    private final int line;
    private final int column;
    private final String detail;

    public SvgDiagnostic(Severity severity, String code, String message, int line, int column, String detail)
    {
        this.severity = severity;
        this.code = code;
        this.message = message;
        this.line = line;
        this.column = column;
        this.detail = detail;
    }

    public SvgDiagnostic(Severity severity, String code, String message)
    {
        this(severity, code, message, 0, 0, null);
    }

    public Severity getSeverity()
    {
        return severity;
    }

    public String getCode()
    {
        return code;
    }

    public String getMessage()
    {
        return message;
    }

    /**
     * @return 1-based line number, or 0 when this diagnostic is not tied to a source position.
     */
    public int getLine()
    {
        return line;
    }

    /**
     * @return 1-based column number, or 0 when this diagnostic is not tied to a source position.
     */
    public int getColumn()
    {
        return column;
    }

    /**
     * @return an optional source excerpt (the offending line with a caret under the column),
     *         or {@code null} when there is none.
     */
    public String getDetail()
    {
        return detail;
    }
}
