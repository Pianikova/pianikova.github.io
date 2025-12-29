/**
 *
 */
package com.e1c.edt.ai.ui;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Optional;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;

public class FileContent
{
    private final IDocument document;
    private final IFile file;

    public FileContent(IDocument document, IFile file)
    {
        this.document = document;
        this.file = file;
    }

    public IDocument tryGetDocument()
    {
        return document;
    }

    public IFile getFile()
    {
        return file;
    }

    public Charset getCharset()
    {
        try
        {
            return Charset.forName(file.getCharset());
        }
        catch (CoreException e)
        {
            return Charset.forName(ResourcesPlugin.getEncoding());
        }
    }

    public Optional<InputStream> getInputStream()
    {
        if (document != null)
        {
            var text = document.get();
            var bytes = text.getBytes(getCharset());
            return Optional.of(new ByteArrayInputStream(bytes));
        }

        if (file != null)
        {
            try
            {
                return Optional.ofNullable(file.getContents());
            }
            catch (CoreException e)
            {
                //
            }
        }

        return Optional.empty();
    }

/*
if("org.eclipse.core.runtime.text".equals(file.getContentDescription().getContentType().getId()))

{
// Это текстовый файл
}*/
}
