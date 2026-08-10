/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import com.e1c.edt.ai.tools.SvgDiagnostic.Severity;

/**
 * Builds the model-facing report text and the chat-facing HTML preview for the {@code Svg} tool.
 * Has no dependency on Eclipse, Guice, or {@code IMarkdownUtils}, so it is unit-testable as a
 * plain object; the caller supplies any already-formatted header HTML (file links, etc).
 */
@SuppressWarnings("nls")
public final class SvgReport
{
    private SvgReport()
    {
    }

    /**
     * @return a {@code data:image/svg+xml;base64,...} URI for the given sanitized SVG markup.
     */
    public static String toDataUri(String sanitizedSvg)
    {
        byte[] bytes = sanitizedSvg.getBytes(StandardCharsets.UTF_8);
        // Standard (non-MIME) encoder: the MIME encoder inserts CRLF every 76 characters, which
        // would break the data URI.
        String base64 = Base64.getEncoder().encodeToString(bytes);
        return "data:image/svg+xml;base64," + base64;
    }

    /**
     * Escapes {@code & < > "} for safe inclusion in HTML. Must never be routed through
     * {@code IMarkdownUtils.escapeForMarkdown}, which escapes {@code + [ ] ( ) !} and would
     * corrupt both the HTML markup and the base64 alphabet.
     */
    public static String escapeHtml(String s)
    {
        if (s == null)
        {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++)
        {
            char c = s.charAt(i);
            switch (c)
            {
                case '&':
                    sb.append("&amp;");
                    break;
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '"':
                    sb.append("&quot;");
                    break;
                default:
                    sb.append(c);
                    break;
            }
        }
        return sb.toString();
    }

    /**
     * @return the model-facing text for {@code action: "save"}.
     */
    public static String buildSaveContent(String path, SvgSanitizeResult result, boolean outsideProject)
    {
        if (!result.isValid())
        {
            return buildInvalidReport("SVG is not well-formed XML and cannot be saved.", result)
                + "Nothing was written to disk. Fix the markup and call Svg again.";
        }

        SvgMetadata m = result.getMetadata();
        StringBuilder sb = new StringBuilder();
        sb.append("SVG saved: \"").append(path).append("\" (").append(formatSize(m.getSizeBytes())).append(", ")
            .append(formatElementCount(m)).append(", ").append(formatViewBox(m)).append(").\n");

        List<SvgDiagnostic> warnings = warningsOf(result);
        if (!warnings.isEmpty())
        {
            sb.append(warnings.size()).append(" issue(s) were fixed automatically before saving:\n");
            appendDiagnosticLines(sb, warnings);
            sb.append("The saved file contains the corrected markup. Use it as the basis for any further edit.\n");
        }
        if (outsideProject)
        {
            sb.append("⚠️ WARNING: File not part of project. "
                + "Changes to non-project files may have irreversible consequences.\n");
        }
        sb.append("Preview is displayed in the chat.");
        return sb.toString();
    }

    /**
     * @return the model-facing text for {@code action: "check"}.
     */
    public static String buildCheckContent(SvgSanitizeResult result)
    {
        if (!result.isValid())
        {
            return buildInvalidReport("SVG is not well-formed XML and cannot be saved.", result)
                + "Nothing was written to disk. Fix the markup and call Svg again.";
        }

        SvgMetadata m = result.getMetadata();
        StringBuilder sb = new StringBuilder();
        sb.append("SVG is valid. ").append(formatElementCount(m)).append(", ").append(formatViewBox(m)).append(", ")
            .append(formatWidthHeight(m)).append(", <style>: ").append(m.hasStyleElement() ? "yes" : "no")
            .append(".\n");

        if (!m.getTopLevelElements().isEmpty())
        {
            sb.append("Top-level elements: ").append(formatTopLevel(m.getTopLevelElements())).append(".\n");
        }

        List<SvgDiagnostic> warnings = warningsOf(result);
        if (warnings.isEmpty())
        {
            sb.append("Nothing needed fixing. Nothing was written to disk.");
        }
        else
        {
            sb.append(warnings.size()).append(" issue(s) would be fixed automatically:\n");
            appendDiagnosticLines(sb, warnings);
            sb.append("Apply these fixes to your markup, then call Svg with action \"save\".");
        }
        return sb.toString();
    }

    /**
     * @return the model-facing text for {@code action: "preview"}.
     */
    public static String buildPreviewContent(String path, SvgSanitizeResult result)
    {
        if (!result.isValid())
        {
            return buildInvalidReport(
                "The file \"" + path + "\" does not contain valid SVG and cannot be previewed.", result);
        }

        SvgMetadata m = result.getMetadata();
        StringBuilder sb = new StringBuilder();
        sb.append("SVG preview displayed: \"").append(path).append("\" (").append(formatSize(m.getSizeBytes()))
            .append(", ").append(formatElementCount(m)).append(", ").append(formatViewBox(m)).append(").");

        List<SvgDiagnostic> warnings = warningsOf(result);
        if (!warnings.isEmpty())
        {
            sb.append("\nThe file on disk contains ").append(warnings.size())
                .append(" construct(s) that were not rendered:\n");
            appendDiagnosticLines(sb, warnings);
            sb.append("Note: `preview` does not modify the file on disk.");
        }
        return sb.toString();
    }

    /**
     * Builds the HTML shown to the user in the chat: an already-formatted header (typically a
     * file link built by the caller via {@code IMarkdownUtils}), an optional caption, an inline
     * {@code <img>} data-URI preview (omitted above {@link McpToolConstants#SVG_MAX_PREVIEW_BYTES}),
     * a metadata line, and the list of automatic fixes, if any.
     *
     * <p>Never route the result through {@code IMarkdownUtils.escapeForMarkdown}: it escapes
     * {@code + [ ] ( ) !}, which corrupts both the base64 alphabet and this HTML.
     */
    public static String buildResponseMarkdown(String headerHtml, SvgSanitizeResult result, String title)
    {
        StringBuilder md = new StringBuilder();
        md.append(headerHtml);

        if (!result.isValid())
        {
            return md.toString();
        }

        md.append("\n\n");
        if (title != null && !title.isBlank())
        {
            md.append("<div style=\"font-weight:600;margin-bottom:4px;\">").append(escapeHtml(title))
                .append("</div>\n");
        }

        SvgMetadata m = result.getMetadata();
        if (m.getSizeBytes() <= McpToolConstants.SVG_MAX_PREVIEW_BYTES)
        {
            md.append("<img alt=\"SVG preview\" style=\"max-width:100%;max-height:480px;height:auto;")
                .append("background:#ffffff;border:1px solid rgba(128,128,128,0.35);border-radius:4px;padding:4px;\" ")
                .append("src=\"").append(toDataUri(result.getSanitizedSource())).append("\" />");
        }
        else
        {
            md.append("<i>Preview omitted: ").append(formatSize(m.getSizeBytes()))
                .append(" exceeds the inline limit of ").append(formatSize(McpToolConstants.SVG_MAX_PREVIEW_BYTES))
                .append(".</i>");
        }

        md.append("\n\n").append(formatMetaLineHtml(m));

        List<SvgDiagnostic> warnings = warningsOf(result);
        if (!warnings.isEmpty())
        {
            md.append("\n\n<div style=\"color:#b06000;font-weight:600;\">Fixed automatically</div>\n<ul>");
            int max = McpToolConstants.SVG_MAX_DIAGNOSTICS;
            int shown = Math.min(warnings.size(), max);
            for (int i = 0; i < shown; i++)
            {
                md.append("<li>").append(escapeHtml(warnings.get(i).getMessage())).append("</li>");
            }
            md.append("</ul>");
            if (warnings.size() > max)
            {
                md.append("<div>... and ").append(warnings.size() - max).append(" more.</div>");
            }
        }

        return md.toString();
    }

    private static String buildInvalidReport(String subject, SvgSanitizeResult result)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(subject).append('\n');
        appendErrorLines(sb, result);
        return sb.toString();
    }

    private static void appendErrorLines(StringBuilder sb, SvgSanitizeResult result)
    {
        List<SvgDiagnostic> errors = new ArrayList<>();
        for (SvgDiagnostic d : result.getDiagnostics())
        {
            if (d.getSeverity() == Severity.ERROR)
            {
                errors.add(d);
            }
        }
        int max = McpToolConstants.SVG_MAX_DIAGNOSTICS;
        int shown = Math.min(errors.size(), max);
        for (int i = 0; i < shown; i++)
        {
            SvgDiagnostic d = errors.get(i);
            if (d.getLine() > 0)
            {
                sb.append("line ").append(d.getLine()).append(", column ").append(d.getColumn()).append(": ")
                    .append(d.getMessage()).append('\n');
                if (d.getDetail() != null)
                {
                    sb.append(d.getDetail()).append('\n');
                }
            }
            else
            {
                sb.append(d.getMessage()).append('\n');
            }
        }
        if (errors.size() > max)
        {
            sb.append("... and ").append(errors.size() - max).append(" more.\n");
        }
    }

    private static void appendDiagnosticLines(StringBuilder sb, List<SvgDiagnostic> diagnostics)
    {
        int max = McpToolConstants.SVG_MAX_DIAGNOSTICS;
        int shown = Math.min(diagnostics.size(), max);
        for (int i = 0; i < shown; i++)
        {
            sb.append(formatWarningLine(diagnostics.get(i))).append('\n');
        }
        if (diagnostics.size() > max)
        {
            sb.append("... and ").append(diagnostics.size() - max).append(" more.\n");
        }
    }

    private static String formatWarningLine(SvgDiagnostic d)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("- ").append(d.getCode());
        if (d.getLine() > 0)
        {
            sb.append(" line ").append(d.getLine());
        }
        sb.append(": ").append(d.getMessage());
        return sb.toString();
    }

    private static List<SvgDiagnostic> warningsOf(SvgSanitizeResult result)
    {
        List<SvgDiagnostic> warnings = new ArrayList<>();
        for (SvgDiagnostic d : result.getDiagnostics())
        {
            if (d.getSeverity() == Severity.WARNING)
            {
                warnings.add(d);
            }
        }
        return warnings;
    }

    private static String formatMetaLineHtml(SvgMetadata m)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("<span style=\"color:gray;opacity:0.7;font-size:0.85em;\">");
        sb.append(escapeHtml(formatViewBox(m))).append(" &middot; ").append(formatElementCount(m))
            .append(" &middot; ").append(formatSize(m.getSizeBytes()));
        sb.append("</span>");
        return sb.toString();
    }

    static String formatSize(int bytes)
    {
        if (bytes < 1024)
        {
            return bytes + " B";
        }
        return String.format(java.util.Locale.ROOT, "%.1f KB", bytes / 1024.0);
    }

    private static String formatElementCount(SvgMetadata m)
    {
        int count = m.getElementCount();
        return count + " element" + (count == 1 ? "" : "s");
    }

    private static String formatViewBox(SvgMetadata m)
    {
        return m.getViewBox() != null ? "viewBox \"" + m.getViewBox() + "\"" : "no viewBox";
    }

    private static String formatWidthHeight(SvgMetadata m)
    {
        if (m.getWidth() == null && m.getHeight() == null)
        {
            return "no fixed width/height";
        }
        return "width " + (m.getWidth() != null ? m.getWidth() : "-") + ", height "
            + (m.getHeight() != null ? m.getHeight() : "-");
    }

    private static String formatTopLevel(Map<String, Integer> topLevel)
    {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, Integer> e : topLevel.entrySet())
        {
            if (!first)
            {
                sb.append(", ");
            }
            sb.append(e.getKey()).append(" (").append(e.getValue()).append(")");
            first = false;
        }
        return sb.toString();
    }
}
