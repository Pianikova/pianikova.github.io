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

import com.e1c.edt.ai.assistent.model.ProjectId;

public class FileContent
    implements IFileContent
{
    private final IDocument document;
    private final IFile file;

    public FileContent(IDocument document, IFile file)
    {
        this.document = document;
        this.file = file;
    }

    @Override
    public ProjectId getProjectId()
    {
        return new ProjectId(file.getProject());
    }

    @Override
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

    @Override
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

    @Override
    public String toString()
    {
        return file.getProjectRelativePath().toPortableString();
    }

/*
if("org.eclipse.core.runtime.text".equals(file.getContentDescription().getContentType().getId()))

{
// Это текстовый файл
}*/
}
