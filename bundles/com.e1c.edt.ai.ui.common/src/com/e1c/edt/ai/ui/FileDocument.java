/**
 *
 */
package com.e1c.edt.ai.ui;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;

import com.e1c.edt.ai.assistent.model.ProjectId;
import com.google.common.base.Preconditions;

public class FileDocument
    implements IFileDocument
{
    private final IDocument document;
    private final IFile file;
    private boolean isOpened;

    public FileDocument(IDocument document, IFile file, boolean isOpened)
    {
        Preconditions.checkNotNull(document);
        Preconditions.checkNotNull(file);

        this.document = document;
        this.file = file;
        this.isOpened = isOpened;
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
    public IFile getFile()
    {
        return file;
    }

    @Override
    public IDocument getDocument()
    {
        return document;
    }

    @Override
    public void save() throws CoreException
    {
        if (!isOpened)
        {
            var content = document.get();
            var bytes = content.getBytes(getCharset());
            file.setContents(new ByteArrayInputStream(bytes), true, true, null);
        }
    }
}
