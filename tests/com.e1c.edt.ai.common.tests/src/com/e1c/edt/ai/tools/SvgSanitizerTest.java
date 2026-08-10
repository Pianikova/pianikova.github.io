/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("nls")
public class SvgSanitizerTest
{
    private ISvgSanitizer sanitizer;

    @Before
    public void setUp()
    {
        sanitizer = new SvgSanitizer();
    }

    private static boolean hasCode(SvgSanitizeResult result, String code)
    {
        for (SvgDiagnostic d : result.getDiagnostics())
        {
            if (code.equals(d.getCode()))
            {
                return true;
            }
        }
        return false;
    }

    @Test
    public void testValidMinimalSvg()
    {
        var result = sanitizer.sanitize(
            "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 10 10\"><rect width=\"5\" height=\"5\"/></svg>");

        assertTrue(result.isValid());
        assertTrue(result.getDiagnostics().isEmpty());
        assertEquals("0 0 10 10", result.getMetadata().getViewBox());
        assertEquals(2, result.getMetadata().getElementCount());
    }

    @Test
    public void testUnclosedTagReportsLineAndColumn()
    {
        var source = "<svg xmlns=\"http://www.w3.org/2000/svg\">\n" + "  <rect width=\"5\"/>\n" + "  <text>A & B</text>\n"
            + "</svg>";
        var result = sanitizer.sanitize(source);

        assertFalse(result.isValid());
        var error = result.getDiagnostics().get(0);
        assertEquals(SvgDiagnostic.PARSE_ERROR, error.getCode());
        assertEquals(3, error.getLine());
        assertTrue(error.getColumn() > 0);
    }

    @Test
    public void testDoctypeBlankingPreservesLineNumberOfLaterError()
    {
        var source = "\n"
            + "<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\" \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">\n"
            + "<svg xmlns=\"http://www.w3.org/2000/svg\">\n" + "  <rect width=\"5\"/>\n" + "  <rect width=\"5\"/>\n"
            + "  <text>A & B</text>\n" + "</svg>";
        var result = sanitizer.sanitize(source);

        assertFalse(result.isValid());
        var error = result.getDiagnostics().get(result.getDiagnostics().size() - 1);
        assertEquals(SvgDiagnostic.PARSE_ERROR, error.getCode());
        assertEquals(6, error.getLine());
    }

    @Test
    public void testRootIsHtml()
    {
        var result = sanitizer.sanitize("<html><body>hi</body></html>");

        assertFalse(result.isValid());
        assertTrue(hasCode(result, SvgDiagnostic.NOT_SVG_ROOT));
    }

    @Test
    public void testRootInXhtmlNamespace()
    {
        var result = sanitizer.sanitize("<svg xmlns=\"http://www.w3.org/1999/xhtml\"><rect/></svg>");

        assertFalse(result.isValid());
        assertTrue(hasCode(result, SvgDiagnostic.NOT_SVG_ROOT));
    }

    @Test
    public void testMissingXmlnsIsFixed()
    {
        var result = sanitizer.sanitize("<svg viewBox=\"0 0 1 1\"><rect/></svg>");

        assertTrue(result.isValid());
        assertTrue(hasCode(result, SvgDiagnostic.MISSING_XMLNS_FIXED));
        assertTrue(result.getSanitizedSource().contains("xmlns=\"http://www.w3.org/2000/svg\""));
    }

    @Test
    public void testMissingXlinkNamespaceIsAdded()
    {
        var result = sanitizer.sanitize("<svg xmlns=\"http://www.w3.org/2000/svg\">"
            + "<rect id=\"a\"/><use xlink:href=\"#a\"/></svg>");

        assertTrue(result.isValid());
        assertTrue(hasCode(result, SvgDiagnostic.XLINK_NS_ADDED));
    }

    @Test
    public void testCodeFenceIsStripped()
    {
        var result = sanitizer
            .sanitize("```svg\n<svg xmlns=\"http://www.w3.org/2000/svg\"><rect/></svg>\n```");

        assertTrue(result.isValid());
        assertTrue(hasCode(result, SvgDiagnostic.CODE_FENCE_REMOVED));
    }

    @Test
    public void testEmptyAndBlankAndNullSource()
    {
        assertFalse(sanitizer.sanitize(null).isValid());
        assertFalse(sanitizer.sanitize("").isValid());
        assertFalse(sanitizer.sanitize("   \n  ").isValid());
        assertTrue(hasCode(sanitizer.sanitize(null), SvgDiagnostic.EMPTY_SOURCE));
    }

    @Test(timeout = 2000)
    public void testOversizedSourceIsRejectedBeforeParsing()
    {
        var big = "a".repeat(McpToolConstants.SVG_MAX_SOURCE_CHARS + 1);
        var result = sanitizer.sanitize(big);

        assertFalse(result.isValid());
        assertTrue(hasCode(result, SvgDiagnostic.TOO_LARGE));
    }

    @Test
    public void testTooManyElements()
    {
        var sb = new StringBuilder("<svg xmlns=\"http://www.w3.org/2000/svg\">");
        for (int i = 0; i < 6000; i++)
        {
            sb.append("<rect/>");
        }
        sb.append("</svg>");

        var result = sanitizer.sanitize(sb.toString());

        assertFalse(result.isValid());
        assertTrue(hasCode(result, SvgDiagnostic.TOO_MANY_ELEMENTS));
    }

    @Test
    public void testTextWhitespaceRoundTrip()
    {
        var result = sanitizer.sanitize("<svg xmlns=\"http://www.w3.org/2000/svg\"><text>  a  b  </text></svg>");

        assertTrue(result.isValid());
        assertTrue(result.getSanitizedSource().contains("<text>  a  b  </text>"));
    }

    @Test
    public void testSerializedOutputHasExactlyOneXmlDeclaration()
    {
        var result = sanitizer
            .sanitize("<?xml version=\"1.0\"?><svg xmlns=\"http://www.w3.org/2000/svg\"><rect/></svg>");

        assertTrue(result.isValid());
        var out = result.getSanitizedSource();
        assertEquals(out.indexOf("<?xml"), out.lastIndexOf("<?xml"));
    }

    @Test
    public void testMetadata()
    {
        var result = sanitizer.sanitize("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 1 1\">"
            + "<defs/><g/><g/><text>x</text></svg>");

        assertTrue(result.isValid());
        var m = result.getMetadata();
        assertNull(m.getWidth());
        assertNull(m.getHeight());
        assertFalse(m.hasStyleElement());
        assertEquals(Integer.valueOf(1), m.getTopLevelElements().get("defs"));
        assertEquals(Integer.valueOf(2), m.getTopLevelElements().get("g"));
        assertEquals(Integer.valueOf(1), m.getTopLevelElements().get("text"));
    }

    @Test
    public void testNoViewBoxWarns()
    {
        var result = sanitizer.sanitize("<svg xmlns=\"http://www.w3.org/2000/svg\"><rect/></svg>");

        assertTrue(result.isValid());
        assertTrue(hasCode(result, SvgDiagnostic.NO_VIEWBOX));
    }

    @Test
    public void testProcessingInstructionIsRemoved()
    {
        var result = sanitizer.sanitize("<?xml version=\"1.0\"?><?xml-stylesheet type=\"text/css\" href=\"x.css\"?>"
            + "<svg xmlns=\"http://www.w3.org/2000/svg\"><rect/></svg>");

        assertTrue(result.isValid());
        assertTrue(hasCode(result, SvgDiagnostic.PROCESSING_INSTRUCTION_REMOVED));
        assertFalse(result.getSanitizedSource().contains("xml-stylesheet"));
    }

    @Test
    public void testStyleElementWithImportIsRemovedButPlainStyleIsKept()
    {
        var withImport = sanitizer.sanitize("<svg xmlns=\"http://www.w3.org/2000/svg\">"
            + "<style>@import url(http://evil/x.css);</style><rect/></svg>");
        assertTrue(withImport.isValid());
        assertTrue(hasCode(withImport, SvgDiagnostic.STYLE_REMOVED));
        assertFalse(withImport.getMetadata().hasStyleElement());

        var plain = sanitizer
            .sanitize("<svg xmlns=\"http://www.w3.org/2000/svg\"><style>.a{fill:red}</style><rect/></svg>");
        assertTrue(plain.isValid());
        assertFalse(hasCode(plain, SvgDiagnostic.STYLE_REMOVED));
        assertTrue(plain.getMetadata().hasStyleElement());
    }
}
