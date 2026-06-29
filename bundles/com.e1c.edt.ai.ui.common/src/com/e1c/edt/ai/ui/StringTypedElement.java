/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.eclipse.compare.IEncodedStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.swt.graphics.Image;

/**
 * Read-only {@link ITypedElement} wrapping an in-memory text snapshot, for use as a side of an
 * Eclipse compare. The element type is derived from the file extension so the compare framework
 * picks a matching text merge viewer (with syntax coloring where available).
 */
public class StringTypedElement
    implements ITypedElement, IEncodedStreamContentAccessor
{
    private final String name;
    private final String type;
    private final byte[] content;

    /**
     * @param name display name of the element (typically the file name), may be {@code null}
     * @param fileName file name used to derive the element type (extension), may be {@code null}
     * @param content the text content, may be {@code null} (treated as empty)
     */
    public StringTypedElement(String name, String fileName, String content)
    {
        this.name = name == null ? "" : name; //$NON-NLS-1$
        this.type = extractType(fileName);
        this.content = (content == null ? "" : content).getBytes(StandardCharsets.UTF_8); //$NON-NLS-1$
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public Image getImage()
    {
        return null;
    }

    @Override
    public String getType()
    {
        return type;
    }

    @Override
    public InputStream getContents()
    {
        return new ByteArrayInputStream(content);
    }

    @Override
    public String getCharset()
    {
        return StandardCharsets.UTF_8.name();
    }

    private static String extractType(String fileName)
    {
        if (fileName != null)
        {
            int dot = fileName.lastIndexOf('.');
            if (dot >= 0 && dot < fileName.length() - 1)
            {
                return fileName.substring(dot + 1);
            }
        }
        return ITypedElement.TEXT_TYPE;
    }
}
