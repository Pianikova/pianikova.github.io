/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.e1c.edt.ai.tools.SvgDiagnostic.Severity;

/**
 * Validates and sanitizes model-authored SVG markup: repairs a handful of unambiguous, extremely
 * common mistakes (missing default namespace, a wrapping markdown code fence, a stray DOCTYPE),
 * removes constructs that would be unsafe to render or persist (scripts, event handlers, external
 * references), and extracts structural metadata used for the report. Has no dependency on
 * Eclipse, Guice, or JavaFX, so it can be unit-tested as a plain object.
 *
 * <p>Well-formedness errors are reported with a 1-based line and column taken from the parser's
 * {@link SAXParseException}, because the resulting DOM carries no position information of its own.
 */
@SuppressWarnings("nls")
public class SvgSanitizer implements ISvgSanitizer
{
    private static final String SVG_NS = "http://www.w3.org/2000/svg";

    private static final String XLINK_NS = "http://www.w3.org/1999/xlink";

    private static final Set<String> ANIMATION_ELEMENTS =
        new HashSet<>(Arrays.asList("set", "animate", "animatetransform", "animatemotion"));

    private static final Set<String> URL_PRESENTATION_ATTRS = new HashSet<>(
        Arrays.asList("fill", "stroke", "filter", "mask", "clip-path", "marker", "marker-start", "marker-mid",
            "marker-end"));

    private static final Pattern DEFAULT_XMLNS_PATTERN = Pattern.compile("\\bxmlns\\s*=");

    private static final Pattern XLINK_XMLNS_PATTERN = Pattern.compile("\\bxmlns:xlink\\s*=");

    private static final Pattern DATA_IMAGE_HREF_PATTERN =
        Pattern.compile("^data:image/(png|jpe?g|gif|webp);base64,", Pattern.CASE_INSENSITIVE);

    private static final Pattern URL_FUNCTION_PATTERN =
        Pattern.compile("url\\(\\s*(['\"]?)([^'\")]*)\\1\\s*\\)", Pattern.CASE_INSENSITIVE);

    @Override
    public SvgSanitizeResult sanitize(String source)
    {
        List<SvgDiagnostic> diagnostics = new ArrayList<>();

        if (source == null || source.trim().isEmpty())
        {
            diagnostics.add(new SvgDiagnostic(Severity.ERROR, SvgDiagnostic.EMPTY_SOURCE, "SVG markup is empty."));
            return new SvgSanitizeResult(false, false, null, diagnostics, null);
        }

        String working = source;
        if (!working.isEmpty() && working.charAt(0) == '\uFEFF')
        {
            working = working.substring(1);
        }

        if (working.length() > McpToolConstants.SVG_MAX_SOURCE_CHARS)
        {
            diagnostics.add(new SvgDiagnostic(Severity.ERROR, SvgDiagnostic.TOO_LARGE,
                "SVG markup is " + working.length() + " characters, exceeding the limit of "
                    + McpToolConstants.SVG_MAX_SOURCE_CHARS + " characters."));
            return new SvgSanitizeResult(false, false, null, diagnostics, null);
        }

        Preprocessed preprocessed = preprocess(working);
        diagnostics.addAll(preprocessed.diagnostics);
        boolean repaired = !preprocessed.diagnostics.isEmpty();

        if (preprocessed.notSvgRoot)
        {
            diagnostics.add(new SvgDiagnostic(Severity.ERROR, SvgDiagnostic.NOT_SVG_ROOT,
                "No <svg> root element was found."));
            return new SvgSanitizeResult(false, repaired, null, diagnostics, null);
        }

        ParseOutcome parseOutcome = parse(preprocessed.text);
        if (!parseOutcome.isSuccess())
        {
            String excerpt = formatSourceExcerpt(preprocessed.text, parseOutcome.line, parseOutcome.column);
            diagnostics.add(new SvgDiagnostic(Severity.ERROR, SvgDiagnostic.PARSE_ERROR, parseOutcome.message,
                parseOutcome.line, parseOutcome.column, excerpt));
            return new SvgSanitizeResult(false, repaired, null, diagnostics, null);
        }

        Document doc = parseOutcome.document;
        Element root = doc.getDocumentElement();
        String rootLocalName = root.getLocalName() != null ? root.getLocalName() : root.getNodeName();
        if (!"svg".equals(rootLocalName) || !SVG_NS.equals(root.getNamespaceURI()))
        {
            diagnostics.add(new SvgDiagnostic(Severity.ERROR, SvgDiagnostic.NOT_SVG_ROOT,
                "The root element must be <svg> in the \"" + SVG_NS + "\" namespace."));
            return new SvgSanitizeResult(false, true, null, diagnostics, null);
        }

        int preCount = countElementsIncludingSelf(root);
        if (preCount > McpToolConstants.SVG_MAX_ELEMENTS)
        {
            diagnostics.add(new SvgDiagnostic(Severity.ERROR, SvgDiagnostic.TOO_MANY_ELEMENTS,
                "SVG contains " + preCount + " elements, exceeding the limit of " + McpToolConstants.SVG_MAX_ELEMENTS
                    + "."));
            return new SvgSanitizeResult(false, true, null, diagnostics, null);
        }

        boolean sanitizedSomething = sanitizeDocument(doc, diagnostics);

        if (!root.hasAttribute("viewBox"))
        {
            diagnostics.add(new SvgDiagnostic(Severity.WARNING, SvgDiagnostic.NO_VIEWBOX,
                "Root <svg> has no viewBox; the picture may not scale cleanly in the chat or in an editor."));
        }

        String serialized;
        try
        {
            serialized = serialize(doc);
        }
        catch (TransformerException error)
        {
            diagnostics.add(new SvgDiagnostic(Severity.ERROR, SvgDiagnostic.PARSE_ERROR,
                "Failed to serialize the sanitized SVG: " + error.getMessage()));
            return new SvgSanitizeResult(false, true, null, diagnostics, null);
        }

        SvgMetadata metadata = buildMetadata(doc, serialized);
        return new SvgSanitizeResult(true, repaired || sanitizedSomething, serialized, diagnostics, metadata);
    }

    // ------------------------------------------------------------------------------------------
    // Stage A: pre-parse repair, with line numbers preserved for accurate error reporting.
    // ------------------------------------------------------------------------------------------

    private static final class Preprocessed
    {
        private final String text;

        private final List<SvgDiagnostic> diagnostics;

        private final boolean notSvgRoot;

        Preprocessed(String text, List<SvgDiagnostic> diagnostics, boolean notSvgRoot)
        {
            this.text = text;
            this.diagnostics = diagnostics;
            this.notSvgRoot = notSvgRoot;
        }
    }

    private Preprocessed preprocess(String source)
    {
        List<SvgDiagnostic> diagnostics = new ArrayList<>();
        String s = stripCodeFence(source, diagnostics);
        s = stripDoctype(s, diagnostics);

        int svgTagStart = findSvgTagStart(s);
        if (svgTagStart < 0)
        {
            return new Preprocessed(s, diagnostics, true);
        }

        int tagEnd = findTagEnd(s, svgTagStart);
        if (tagEnd >= 0)
        {
            s = fixNamespaces(s, svgTagStart, tagEnd, diagnostics);
        }

        return new Preprocessed(s, diagnostics, false);
    }

    /**
     * Replaces every non-newline character in {@code [start, end)} with a space, so that line
     * numbers reported by the parser for the rest of the document still match the lines the
     * model originally wrote.
     */
    private static String blankPreservingLines(String s, int start, int end)
    {
        StringBuilder sb = new StringBuilder(s);
        for (int i = start; i < end && i < sb.length(); i++)
        {
            if (sb.charAt(i) != '\n')
            {
                sb.setCharAt(i, ' ');
            }
        }
        return sb.toString();
    }

    private static String stripCodeFence(String s, List<SvgDiagnostic> diagnostics)
    {
        int idx = 0;
        while (idx < s.length() && Character.isWhitespace(s.charAt(idx)))
        {
            idx++;
        }
        if (!s.regionMatches(idx, "```", 0, 3))
        {
            return s;
        }

        int leadingLineEnd = s.indexOf('\n', idx);
        leadingLineEnd = leadingLineEnd < 0 ? s.length() : leadingLineEnd;
        String result = blankPreservingLines(s, 0, leadingLineEnd);

        int trailingFence = result.lastIndexOf("```");
        if (trailingFence > idx)
        {
            int trailingLineEnd = result.indexOf('\n', trailingFence);
            trailingLineEnd = trailingLineEnd < 0 ? result.length() : trailingLineEnd;
            result = blankPreservingLines(result, trailingFence, trailingLineEnd);
        }

        diagnostics.add(new SvgDiagnostic(Severity.WARNING, SvgDiagnostic.CODE_FENCE_REMOVED,
            "A wrapping markdown code fence was removed."));
        return result;
    }

    private static String stripDoctype(String s, List<SvgDiagnostic> diagnostics)
    {
        int doctypeStart = indexOfIgnoreCaseAscii(s, "<!doctype", 0);
        if (doctypeStart < 0)
        {
            return s;
        }

        int bracketDepth = 0;
        int end = -1;
        for (int j = doctypeStart + "<!doctype".length(); j < s.length(); j++)
        {
            char c = s.charAt(j);
            if (c == '[')
            {
                bracketDepth++;
            }
            else if (c == ']')
            {
                bracketDepth--;
            }
            else if (c == '>' && bracketDepth <= 0)
            {
                end = j + 1;
                break;
            }
        }

        if (end <= doctypeStart)
        {
            return s;
        }

        diagnostics.add(new SvgDiagnostic(Severity.WARNING, SvgDiagnostic.DOCTYPE_REMOVED,
            "A DOCTYPE declaration was removed; external and internal DTD subsets are not supported."));
        return blankPreservingLines(s, doctypeStart, end);
    }

    private static int indexOfIgnoreCaseAscii(String s, String needleLower, int from)
    {
        int n = needleLower.length();
        int limit = s.length() - n;
        for (int i = Math.max(from, 0); i <= limit; i++)
        {
            boolean match = true;
            for (int k = 0; k < n; k++)
            {
                if (Character.toLowerCase(s.charAt(i + k)) != needleLower.charAt(k))
                {
                    match = false;
                    break;
                }
            }
            if (match)
            {
                return i;
            }
        }
        return -1;
    }

    private static int findSvgTagStart(String s)
    {
        int idx = 0;
        while (true)
        {
            idx = s.indexOf("<svg", idx);
            if (idx < 0)
            {
                return -1;
            }
            int next = idx + 4;
            char c = next < s.length() ? s.charAt(next) : ' ';
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '>' || c == '/')
            {
                return idx;
            }
            idx = next;
        }
    }

    /**
     * Finds the index of the closing {@code '>'} of the start tag beginning at {@code tagStart},
     * respecting quoted attribute values (which may themselves contain {@code '>'}).
     */
    private static int findTagEnd(String s, int tagStart)
    {
        boolean inSingle = false;
        boolean inDouble = false;
        for (int i = tagStart; i < s.length(); i++)
        {
            char c = s.charAt(i);
            if (c == '\'' && !inDouble)
            {
                inSingle = !inSingle;
            }
            else if (c == '"' && !inSingle)
            {
                inDouble = !inDouble;
            }
            else if (c == '>' && !inSingle && !inDouble)
            {
                return i;
            }
        }
        return -1;
    }

    private static String fixNamespaces(String s, int tagStart, int tagEnd, List<SvgDiagnostic> diagnostics)
    {
        String tagText = s.substring(tagStart, tagEnd + 1);
        boolean hasDefaultXmlns = DEFAULT_XMLNS_PATTERN.matcher(tagText).find();
        boolean hasXlinkXmlns = XLINK_XMLNS_PATTERN.matcher(tagText).find();
        boolean usesXlinkPrefix = s.contains("xlink:");

        StringBuilder insertion = new StringBuilder();
        int line = lineOfOffset(s, tagStart);
        if (usesXlinkPrefix && !hasXlinkXmlns)
        {
            insertion.append(" xmlns:xlink=\"http://www.w3.org/1999/xlink\"");
            diagnostics.add(new SvgDiagnostic(Severity.WARNING, SvgDiagnostic.XLINK_NS_ADDED,
                "The \"xlink\" namespace was declared automatically on the root <svg>.", line, 1, null));
        }
        if (!hasDefaultXmlns)
        {
            insertion.append(" xmlns=\"http://www.w3.org/2000/svg\"");
            diagnostics.add(new SvgDiagnostic(Severity.WARNING, SvgDiagnostic.MISSING_XMLNS_FIXED,
                "The root <svg> had no xmlns; \"http://www.w3.org/2000/svg\" was added.", line, 1, null));
        }

        if (insertion.length() == 0)
        {
            return s;
        }
        int insertAt = tagStart + 4;
        return s.substring(0, insertAt) + insertion + s.substring(insertAt);
    }

    private static int lineOfOffset(String s, int offset)
    {
        int line = 1;
        int limit = Math.min(offset, s.length());
        for (int i = 0; i < limit; i++)
        {
            if (s.charAt(i) == '\n')
            {
                line++;
            }
        }
        return line;
    }

    static String formatSourceExcerpt(String text, int line, int column)
    {
        if (line <= 0)
        {
            return null;
        }
        String[] lines = text.split("\n", -1);
        if (line > lines.length)
        {
            return null;
        }
        String targetLine = lines[line - 1];
        if (targetLine.endsWith("\r"))
        {
            targetLine = targetLine.substring(0, targetLine.length() - 1);
        }

        StringBuilder sb = new StringBuilder();
        String prefix = String.format(Locale.ROOT, "%4d | ", line);
        sb.append(prefix).append(targetLine);
        if (column > 0)
        {
            sb.append('\n');
            for (int i = 0; i < prefix.length(); i++)
            {
                sb.append(' ');
            }
            for (int i = 0; i < column - 1; i++)
            {
                sb.append(' ');
            }
            sb.append('^');
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------------------------------
    // Stage C: XXE-safe parsing.
    // ------------------------------------------------------------------------------------------

    private static final class ParseOutcome
    {
        private final Document document;

        private final int line;

        private final int column;

        private final String message;

        private ParseOutcome(Document document, int line, int column, String message)
        {
            this.document = document;
            this.line = line;
            this.column = column;
            this.message = message;
        }

        static ParseOutcome success(Document document)
        {
            return new ParseOutcome(document, 0, 0, null);
        }

        static ParseOutcome failure(int line, int column, String message)
        {
            return new ParseOutcome(null, line, column, message);
        }

        boolean isSuccess()
        {
            return document != null;
        }
    }

    private static ParseOutcome parse(String preprocessedText)
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try
        {
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        }
        catch (ParserConfigurationException error)
        {
            // Feature not supported by this provider; the attributes and settings below still apply.
        }
        trySetAttribute(factory, XMLConstants.ACCESS_EXTERNAL_DTD, "");
        trySetAttribute(factory, XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        factory.setNamespaceAware(true);
        factory.setValidating(false);

        try
        {
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader("")));
            builder.setErrorHandler(new ErrorHandler()
            {
                @Override
                public void warning(SAXParseException exception)
                {
                    // Not fatal; ignored.
                }

                @Override
                public void error(SAXParseException exception) throws SAXException
                {
                    throw exception;
                }

                @Override
                public void fatalError(SAXParseException exception) throws SAXException
                {
                    throw exception;
                }
            });
            Document doc = builder.parse(new InputSource(new StringReader(preprocessedText)));
            return ParseOutcome.success(doc);
        }
        catch (SAXParseException error)
        {
            return ParseOutcome.failure(error.getLineNumber(), error.getColumnNumber(), error.getMessage());
        }
        catch (SAXException | IOException | ParserConfigurationException error)
        {
            return ParseOutcome.failure(0, 0, String.valueOf(error.getMessage()));
        }
    }

    private static void trySetAttribute(DocumentBuilderFactory factory, String name, Object value)
    {
        try
        {
            factory.setAttribute(name, value);
        }
        catch (IllegalArgumentException error)
        {
            // Attribute not supported by this provider; ignore.
        }
    }

    private static void trySetAttribute(TransformerFactory factory, String name, Object value)
    {
        try
        {
            factory.setAttribute(name, value);
        }
        catch (IllegalArgumentException error)
        {
            // Attribute not supported by this provider; ignore.
        }
    }

    // ------------------------------------------------------------------------------------------
    // Stage F: sanitization walk.
    // ------------------------------------------------------------------------------------------

    private static int countElementsIncludingSelf(Element el)
    {
        int count = 1;
        NodeList children = el.getChildNodes();
        for (int i = 0; i < children.getLength(); i++)
        {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE)
            {
                count += countElementsIncludingSelf((Element)child);
            }
        }
        return count;
    }

    private boolean sanitizeDocument(Document doc, List<SvgDiagnostic> diagnostics)
    {
        List<Node> nodesToRemove = new ArrayList<>();
        List<Attr> attrsToRemove = new ArrayList<>();
        Set<String> warnedNamespaces = new HashSet<>();

        NodeList topChildren = doc.getChildNodes();
        for (int i = 0; i < topChildren.getLength(); i++)
        {
            Node n = topChildren.item(i);
            if (n.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE)
            {
                nodesToRemove.add(n);
                diagnostics.add(new SvgDiagnostic(Severity.WARNING, SvgDiagnostic.PROCESSING_INSTRUCTION_REMOVED,
                    "Processing instruction <?" + n.getNodeName() + "?> removed."));
            }
        }

        walkElement(doc.getDocumentElement(), nodesToRemove, attrsToRemove, diagnostics, warnedNamespaces);

        for (Node n : nodesToRemove)
        {
            if (n.getParentNode() != null)
            {
                n.getParentNode().removeChild(n);
            }
        }
        for (Attr a : attrsToRemove)
        {
            Element owner = a.getOwnerElement();
            if (owner != null)
            {
                owner.removeAttributeNode(a);
            }
        }

        return !nodesToRemove.isEmpty() || !attrsToRemove.isEmpty();
    }

    private void walkElement(Element el, List<Node> nodesToRemove, List<Attr> attrsToRemove,
        List<SvgDiagnostic> diagnostics, Set<String> warnedNamespaces)
    {
        String ns = el.getNamespaceURI();
        if (!SVG_NS.equals(ns))
        {
            nodesToRemove.add(el);
            String key = String.valueOf(ns);
            if (warnedNamespaces.add(key))
            {
                diagnostics.add(new SvgDiagnostic(Severity.WARNING, SvgDiagnostic.FOREIGN_NAMESPACE_REMOVED,
                    "Element(s) in namespace \"" + key + "\" removed; only the SVG namespace is allowed."));
            }
            return;
        }

        String localName = el.getLocalName();
        String lname = localName != null ? localName.toLowerCase(Locale.ROOT) : "";

        if ("script".equals(lname))
        {
            nodesToRemove.add(el);
            diagnostics.add(
                new SvgDiagnostic(Severity.WARNING, SvgDiagnostic.SCRIPT_REMOVED, "<script> element removed."));
            return;
        }

        if ("foreignobject".equals(lname))
        {
            nodesToRemove.add(el);
            diagnostics.add(new SvgDiagnostic(Severity.WARNING, SvgDiagnostic.FOREIGN_OBJECT_REMOVED,
                "<foreignObject> element removed."));
            return;
        }

        if (ANIMATION_ELEMENTS.contains(lname))
        {
            String attributeName = el.hasAttribute("attributeName") ? el.getAttribute("attributeName") : null;
            if (attributeName != null)
            {
                String an = attributeName.trim().toLowerCase(Locale.ROOT);
                if (an.startsWith("on") || "href".equals(an) || "xlink:href".equals(an))
                {
                    nodesToRemove.add(el);
                    diagnostics.add(new SvgDiagnostic(Severity.WARNING, SvgDiagnostic.ANIMATION_REMOVED,
                        "<" + localName + "> targeting \"" + attributeName + "\" removed."));
                    return;
                }
            }
        }

        if ("style".equals(lname) && isUnsafeStyleElementText(el.getTextContent()))
        {
            nodesToRemove.add(el);
            diagnostics.add(new SvgDiagnostic(Severity.WARNING, SvgDiagnostic.STYLE_REMOVED,
                "<style> element removed: contains @import or an external url()."));
            return;
        }

        sanitizeAttributes(el, attrsToRemove, diagnostics);

        NodeList children = el.getChildNodes();
        for (int i = 0; i < children.getLength(); i++)
        {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE)
            {
                walkElement((Element)child, nodesToRemove, attrsToRemove, diagnostics, warnedNamespaces);
            }
            else if (child.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE)
            {
                nodesToRemove.add(child);
                diagnostics.add(new SvgDiagnostic(Severity.WARNING, SvgDiagnostic.PROCESSING_INSTRUCTION_REMOVED,
                    "Processing instruction <?" + child.getNodeName() + "?> removed."));
            }
        }
    }

    private void sanitizeAttributes(Element el, List<Attr> attrsToRemove, List<SvgDiagnostic> diagnostics)
    {
        NamedNodeMap attrs = el.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++)
        {
            Attr a = (Attr)attrs.item(i);
            String qname = a.getName();
            if ("xmlns".equals(qname) || qname.startsWith("xmlns:"))
            {
                continue;
            }

            String localName = a.getLocalName() != null ? a.getLocalName() : qname;
            String lname = localName.toLowerCase(Locale.ROOT);
            String value = a.getValue();
            String display = elementDisplayName(el);

            if (lname.length() > 2 && lname.startsWith("on"))
            {
                attrsToRemove.add(a);
                diagnostics.add(new SvgDiagnostic(Severity.WARNING, SvgDiagnostic.EVENT_ATTRIBUTE_REMOVED,
                    "Attribute \"" + qname + "\" removed from <" + display + ">."));
                continue;
            }

            boolean isHref = "href".equals(lname) && (a.getNamespaceURI() == null || XLINK_NS.equals(a.getNamespaceURI()));
            if (isHref)
            {
                if (!isAllowedHref(value))
                {
                    attrsToRemove.add(a);
                    diagnostics.add(new SvgDiagnostic(Severity.WARNING, categorizeUrlIssue(value),
                        "Attribute \"" + qname + "\"=\"" + value + "\" removed from <" + display + ">."));
                }
                continue;
            }

            if (URL_PRESENTATION_ATTRS.contains(lname))
            {
                String target = extractUrlTarget(value);
                if (target != null && !target.startsWith("#"))
                {
                    attrsToRemove.add(a);
                    diagnostics.add(new SvgDiagnostic(Severity.WARNING, categorizeUrlIssue(target),
                        "Attribute \"" + qname + "\"=\"" + value + "\" removed from <" + display + ">."));
                }
                continue;
            }

            if ("style".equals(lname) && isUnsafeStyleAttributeText(value))
            {
                attrsToRemove.add(a);
                diagnostics.add(new SvgDiagnostic(Severity.WARNING, SvgDiagnostic.UNSAFE_URL_REMOVED,
                    "Attribute \"style\" removed from <" + display + ">: unsafe url() or script reference."));
            }
        }
    }

    private static String elementDisplayName(Element el)
    {
        return el.getLocalName() != null ? el.getLocalName() : el.getTagName();
    }

    private static boolean isAllowedHref(String value)
    {
        if (value == null)
        {
            return true;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#"))
        {
            return true;
        }
        return DATA_IMAGE_HREF_PATTERN.matcher(trimmed).find();
    }

    private static String categorizeUrlIssue(String value)
    {
        if (value == null)
        {
            return SvgDiagnostic.UNSAFE_URL_REMOVED;
        }
        String lower = value.trim().toLowerCase(Locale.ROOT);
        if (lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("//")
            || lower.startsWith("file:") || lower.startsWith("ftp:")
            || (!lower.isEmpty() && !lower.contains(":") && !lower.startsWith("#")))
        {
            return SvgDiagnostic.EXTERNAL_REFERENCE_REMOVED;
        }
        return SvgDiagnostic.UNSAFE_URL_REMOVED;
    }

    private static String extractUrlTarget(String value)
    {
        if (value == null)
        {
            return null;
        }
        Matcher m = URL_FUNCTION_PATTERN.matcher(value);
        if (m.find())
        {
            return m.group(2).trim();
        }
        return null;
    }

    private static boolean isUnsafeStyleElementText(String text)
    {
        if (text == null)
        {
            return false;
        }
        if (text.toLowerCase(Locale.ROOT).contains("@import"))
        {
            return true;
        }
        return hasExternalUrlReference(text);
    }

    private static boolean isUnsafeStyleAttributeText(String text)
    {
        if (text == null)
        {
            return false;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("javascript:") || lower.contains("expression("))
        {
            return true;
        }
        return hasExternalUrlReference(text);
    }

    private static boolean hasExternalUrlReference(String text)
    {
        Matcher m = URL_FUNCTION_PATTERN.matcher(text);
        while (m.find())
        {
            String target = m.group(2).trim();
            if (!target.startsWith("#"))
            {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------------------------------
    // Stage G/H: metadata extraction and serialization.
    // ------------------------------------------------------------------------------------------

    private static SvgMetadata buildMetadata(Document doc, String serialized)
    {
        Element root = doc.getDocumentElement();
        String viewBox = root.hasAttribute("viewBox") ? root.getAttribute("viewBox") : null;
        String width = root.hasAttribute("width") ? root.getAttribute("width") : null;
        String height = root.hasAttribute("height") ? root.getAttribute("height") : null;
        int elementCount = countElementsIncludingSelf(root);
        boolean hasStyle = containsStyleElement(root);

        Map<String, Integer> topLevel = new LinkedHashMap<>();
        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++)
        {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE)
            {
                String name = ((Element)child).getLocalName();
                if (name == null)
                {
                    name = child.getNodeName();
                }
                topLevel.merge(name, 1, Integer::sum);
            }
        }

        int sizeBytes = serialized.getBytes(StandardCharsets.UTF_8).length;
        return new SvgMetadata(viewBox, width, height, elementCount, hasStyle, topLevel, sizeBytes);
    }

    private static boolean containsStyleElement(Element el)
    {
        if ("style".equalsIgnoreCase(el.getLocalName()))
        {
            return true;
        }
        NodeList children = el.getChildNodes();
        for (int i = 0; i < children.getLength(); i++)
        {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE && containsStyleElement((Element)child))
            {
                return true;
            }
        }
        return false;
    }

    private static String serialize(Document doc) throws TransformerException
    {
        TransformerFactory tf = TransformerFactory.newInstance();
        tf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        trySetAttribute(tf, XMLConstants.ACCESS_EXTERNAL_DTD, "");
        trySetAttribute(tf, XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");

        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        // Text content in <text> elements is whitespace-significant: never let the transformer
        // pretty-print, or labels would be silently corrupted.
        transformer.setOutputProperty(OutputKeys.INDENT, "no");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));

        // The transformer's declaration is discarded above; we prepend our own exactly once so the
        // declared encoding always matches the UTF-8 bytes this tool writes to disk.
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + writer + "\n";
    }
}
