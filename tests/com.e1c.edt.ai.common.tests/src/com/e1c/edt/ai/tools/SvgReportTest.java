/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.junit.Test;

import com.e1c.edt.ai.tools.SvgDiagnostic.Severity;

@SuppressWarnings("nls")
public class SvgReportTest
{
    @Test
    public void testToDataUriShape()
    {
        var uri = SvgReport.toDataUri("<svg xmlns=\"http://www.w3.org/2000/svg\"><rect/></svg>");

        assertTrue(uri.startsWith("data:image/svg+xml;base64,"));
        assertFalse(uri.contains("\r"));
        assertFalse(uri.contains("\n"));
        assertFalse(uri.contains("\""));
    }

    @Test
    public void testDiagnosticMessageIsHtmlEscapedInResponseMarkdown()
    {
        var metadata = new SvgMetadata("0 0 1 1", null, null, 1, false, new LinkedHashMap<>(), 20);
        List<SvgDiagnostic> diagnostics = new ArrayList<>();
        diagnostics
            .add(new SvgDiagnostic(Severity.WARNING, SvgDiagnostic.SCRIPT_REMOVED, "<script> element removed."));
        var result = new SvgSanitizeResult(true, true, "<svg xmlns=\"http://www.w3.org/2000/svg\"><rect/></svg>",
            diagnostics, metadata);

        var md = SvgReport.buildResponseMarkdown("<a>header</a>", result, null);

        assertTrue(md.contains("&lt;script&gt;"));
        assertFalse(md.contains("<script>"));
    }

    @Test
    public void testOversizedPreviewIsOmitted()
    {
        var metadata = new SvgMetadata(null, null, null, 1, false, new LinkedHashMap<>(),
            McpToolConstants.SVG_MAX_PREVIEW_BYTES + 1);
        var result = new SvgSanitizeResult(true, false, "<svg xmlns=\"http://www.w3.org/2000/svg\"/>",
            new ArrayList<>(), metadata);

        var md = SvgReport.buildResponseMarkdown("<a>header</a>", result, null);

        assertFalse(md.contains("<img"));
        assertTrue(md.contains("Preview omitted"));
    }

    @Test
    public void testManyDiagnosticsAreCapped()
    {
        List<SvgDiagnostic> diagnostics = new ArrayList<>();
        for (int i = 0; i < 200; i++)
        {
            diagnostics.add(new SvgDiagnostic(Severity.WARNING, SvgDiagnostic.SCRIPT_REMOVED, "removed " + i));
        }
        var metadata = new SvgMetadata(null, null, null, 1, false, new LinkedHashMap<>(), 10);
        var result = new SvgSanitizeResult(true, true, "<svg xmlns=\"http://www.w3.org/2000/svg\"/>", diagnostics,
            metadata);

        var content = SvgReport.buildCheckContent(result);

        assertTrue(content.lines().count() < 100);
        assertTrue(content.contains("... and " + (200 - McpToolConstants.SVG_MAX_DIAGNOSTICS) + " more."));
    }

    @Test
    public void testInvalidCheckContentNamesLineAndColumn()
    {
        var sanitizer = new SvgSanitizer();
        var source = "<svg xmlns=\"http://www.w3.org/2000/svg\">\n<text>A & B</text>\n</svg>";
        var result = sanitizer.sanitize(source);

        var content = SvgReport.buildCheckContent(result);

        assertTrue(content.contains("line"));
        assertTrue(content.contains("column"));
        assertTrue(content.contains("^"));
        assertTrue(content.contains("Nothing was written"));
    }
}
