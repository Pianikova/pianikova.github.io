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

/**
 * Adversarial input coverage for {@link SvgSanitizer}: XXE / entity expansion attacks and every
 * unsafe-construct removal rule.
 */
@SuppressWarnings("nls")
public class SvgSanitizerSecurityTest
{
    private ISvgSanitizer sanitizer;

    @Before
    public void setUp()
    {
        sanitizer = new SvgSanitizer();
    }

    private static boolean hasCode(SvgSanitizeResult result, String code)
    {
        return countCode(result, code) > 0;
    }

    private static long countCode(SvgSanitizeResult result, String code)
    {
        long count = 0;
        for (SvgDiagnostic d : result.getDiagnostics())
        {
            if (code.equals(d.getCode()))
            {
                count++;
            }
        }
        return count;
    }

    @Test(timeout = 5000)
    public void testBillionLaughsDoesNotHangAndIsRejected()
    {
        var source = "<!DOCTYPE lolz [\n" + " <!ENTITY lol \"lol\">\n"
            + " <!ENTITY lol2 \"&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;\">\n"
            + " <!ENTITY lol3 \"&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;\">\n" + "]>\n"
            + "<svg xmlns=\"http://www.w3.org/2000/svg\">&lol3;</svg>";

        var result = sanitizer.sanitize(source);

        assertFalse(result.isValid());
        assertNull(result.getSanitizedSource());
    }

    @Test(timeout = 5000)
    public void testXxeFileReferenceIsDefused()
    {
        var source = "<!DOCTYPE svg [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]>\n"
            + "<svg xmlns=\"http://www.w3.org/2000/svg\">&xxe;</svg>";

        var result = sanitizer.sanitize(source);

        assertFalse(result.isValid());
        assertNull(result.getSanitizedSource());
    }

    @Test(timeout = 5000)
    public void testExternalParameterEntityTriggersNoNetworkAccess()
    {
        var source = "<!DOCTYPE svg [\n" + "<!ENTITY % remote SYSTEM \"http://127.0.0.1:1/evil.dtd\">\n"
            + "%remote;\n" + "]>\n" + "<svg xmlns=\"http://www.w3.org/2000/svg\"/>";

        // The assertion that matters is the @Test(timeout): if this attempted the network call,
        // it would hang or fail slowly instead of returning promptly.
        var result = sanitizer.sanitize(source);
        assertTrue(result.isValid());
    }

    @Test
    public void testScriptElementsRemovedAtAnyDepth()
    {
        var result = sanitizer.sanitize("<svg xmlns=\"http://www.w3.org/2000/svg\">" + "<script>alert(1)</script>"
            + "<defs><script>alert(2)</script></defs>" + "<rect/></svg>");

        assertTrue(result.isValid());
        assertEquals(2, countCode(result, SvgDiagnostic.SCRIPT_REMOVED));
        assertFalse(result.getSanitizedSource().toLowerCase(java.util.Locale.ROOT).contains("script"));
    }

    @Test
    public void testEventAttributesRemovedElementsSurvive()
    {
        var result = sanitizer.sanitize("<svg xmlns=\"http://www.w3.org/2000/svg\">" + "<rect onclick=\"a()\"/>"
            + "<circle onload=\"b()\" r=\"1\"/>" + "<rect onmouseover=\"c()\"/></svg>");

        assertTrue(result.isValid());
        assertEquals(3, countCode(result, SvgDiagnostic.EVENT_ATTRIBUTE_REMOVED));
        var out = result.getSanitizedSource();
        assertFalse(out.contains("onclick"));
        assertFalse(out.contains("onload"));
        assertFalse(out.contains("onmouseover"));
        assertTrue(out.contains("<rect"));
        assertTrue(out.contains("<circle"));
    }

    @Test
    public void testJavascriptHrefRemovedInBothForms()
    {
        var result = sanitizer.sanitize(
            "<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\">"
                + "<a href=\"javascript:alert(1)\"><rect/></a>" + "<a xlink:href=\"javascript:alert(1)\"><rect/></a></svg>");

        assertTrue(result.isValid());
        assertFalse(result.getSanitizedSource().contains("javascript:"));
    }

    @Test
    public void testTabInjectedSchemeIsRemovedByAllowlist()
    {
        var result = sanitizer.sanitize(
            "<svg xmlns=\"http://www.w3.org/2000/svg\"><a href=\"java&#9;script:alert(1)\"><rect/></a></svg>");

        assertTrue(result.isValid());
        assertFalse(result.getSanitizedSource().contains("script:alert"));
    }

    @Test
    public void testDataTextHtmlAndDataImageSvgHrefsAreRemoved()
    {
        var result = sanitizer.sanitize("<svg xmlns=\"http://www.w3.org/2000/svg\">"
            + "<a href=\"data:text/html,hi\"><rect/></a>"
            + "<image href=\"data:image/svg+xml;base64,PHN2Zz48L3N2Zz4=\"/></svg>");

        assertTrue(result.isValid());
        assertFalse(result.getSanitizedSource().contains("data:text/html"));
        assertFalse(result.getSanitizedSource().contains("data:image/svg+xml"));
    }

    @Test
    public void testDataImagePngHrefIsKept()
    {
        var result = sanitizer.sanitize(
            "<svg xmlns=\"http://www.w3.org/2000/svg\"><image href=\"data:image/png;base64,iVBORw0KGgo=\"/></svg>");

        assertTrue(result.isValid());
        assertTrue(result.getSanitizedSource().contains("data:image/png;base64,iVBORw0KGgo="));
    }

    @Test
    public void testExternalImageHrefRemovedElementKept()
    {
        var result = sanitizer
            .sanitize("<svg xmlns=\"http://www.w3.org/2000/svg\"><image href=\"https://evil/x.png\"/></svg>");

        assertTrue(result.isValid());
        assertTrue(hasCode(result, SvgDiagnostic.EXTERNAL_REFERENCE_REMOVED));
        assertFalse(result.getSanitizedSource().contains("evil"));
        assertTrue(result.getSanitizedSource().contains("<image"));
    }

    @Test
    public void testUseFragmentHrefIsUntouched()
    {
        var result = sanitizer
            .sanitize("<svg xmlns=\"http://www.w3.org/2000/svg\"><rect id=\"a\"/><use href=\"#a\"/></svg>");

        assertTrue(result.isValid());
        assertTrue(result.getSanitizedSource().contains("href=\"#a\""));
        assertFalse(hasCode(result, SvgDiagnostic.EXTERNAL_REFERENCE_REMOVED));
        assertFalse(hasCode(result, SvgDiagnostic.UNSAFE_URL_REMOVED));
    }

    @Test
    public void testForeignObjectIsRemoved()
    {
        var result = sanitizer.sanitize(
            "<svg xmlns=\"http://www.w3.org/2000/svg\"><foreignObject><div>hi</div></foreignObject><rect/></svg>");

        assertTrue(result.isValid());
        assertTrue(hasCode(result, SvgDiagnostic.FOREIGN_OBJECT_REMOVED));
        assertFalse(result.getSanitizedSource().toLowerCase(java.util.Locale.ROOT).contains("foreignobject"));
    }

    @Test
    public void testForeignNamespaceElementIsRemoved()
    {
        var result = sanitizer.sanitize(
            "<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:html=\"http://www.w3.org/1999/xhtml\">"
                + "<html:script>alert(1)</html:script><rect/></svg>");

        assertTrue(result.isValid());
        assertTrue(hasCode(result, SvgDiagnostic.FOREIGN_NAMESPACE_REMOVED));
        assertFalse(result.getSanitizedSource().contains("alert(1)"));
    }

    @Test
    public void testUrlPresentationAttributeExternalVsFragment()
    {
        var result = sanitizer.sanitize("<svg xmlns=\"http://www.w3.org/2000/svg\">"
            + "<rect fill=\"url(http://evil/x)\"/>" + "<rect fill=\"url(#grad)\"/></svg>");

        assertTrue(result.isValid());
        assertTrue(hasCode(result, SvgDiagnostic.EXTERNAL_REFERENCE_REMOVED));
        assertTrue(result.getSanitizedSource().contains("fill=\"url(#grad)\""));
        assertFalse(result.getSanitizedSource().contains("evil"));
    }

    @Test
    public void testStyleAttributeWithUnsafeUrlIsRemoved()
    {
        var result = sanitizer.sanitize(
            "<svg xmlns=\"http://www.w3.org/2000/svg\"><rect style=\"background:url(http://evil/x)\"/></svg>");

        assertTrue(result.isValid());
        assertTrue(hasCode(result, SvgDiagnostic.UNSAFE_URL_REMOVED));
        assertFalse(result.getSanitizedSource().contains("style"));
    }

    @Test
    public void testAnimationTargetingEventAttributeIsRemoved()
    {
        var result = sanitizer.sanitize("<svg xmlns=\"http://www.w3.org/2000/svg\">"
            + "<rect><set attributeName=\"onclick\" to=\"alert(1)\"/></rect></svg>");

        assertTrue(result.isValid());
        assertTrue(hasCode(result, SvgDiagnostic.ANIMATION_REMOVED));
        assertFalse(result.getSanitizedSource().contains("<set"));
    }
}
