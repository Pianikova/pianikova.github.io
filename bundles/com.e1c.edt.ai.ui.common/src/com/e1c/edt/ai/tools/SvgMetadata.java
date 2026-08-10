/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.util.Collections;
import java.util.Map;

/**
 * Structural facts about a sanitized SVG document, used to build the report shown to the model
 * and to the user. All values are read from the document as parsed; {@code null} means the
 * corresponding attribute was absent.
 */
public final class SvgMetadata
{
    private final String viewBox;
    private final String width;
    private final String height;
    private final int elementCount;
    private final boolean hasStyleElement;
    private final Map<String, Integer> topLevelElements;
    private final int sizeBytes;

    public SvgMetadata(String viewBox, String width, String height, int elementCount, boolean hasStyleElement,
        Map<String, Integer> topLevelElements, int sizeBytes)
    {
        this.viewBox = viewBox;
        this.width = width;
        this.height = height;
        this.elementCount = elementCount;
        this.hasStyleElement = hasStyleElement;
        this.topLevelElements = Collections.unmodifiableMap(topLevelElements);
        this.sizeBytes = sizeBytes;
    }

    /**
     * @return the root {@code viewBox} attribute value, or {@code null} if absent.
     */
    public String getViewBox()
    {
        return viewBox;
    }

    /**
     * @return the root {@code width} attribute value, or {@code null} if absent.
     */
    public String getWidth()
    {
        return width;
    }

    /**
     * @return the root {@code height} attribute value, or {@code null} if absent.
     */
    public String getHeight()
    {
        return height;
    }

    public int getElementCount()
    {
        return elementCount;
    }

    public boolean hasStyleElement()
    {
        return hasStyleElement;
    }

    /**
     * @return an unmodifiable, insertion-ordered map of local element name to occurrence count,
     *         for elements that are direct children of the root {@code <svg>}.
     */
    public Map<String, Integer> getTopLevelElements()
    {
        return topLevelElements;
    }

    /**
     * @return the UTF-8 encoded size, in bytes, of the sanitized markup.
     */
    public int getSizeBytes()
    {
        return sizeBytes;
    }
}
