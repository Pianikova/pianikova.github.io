/**
 *
 */
package com.e1c.edt.ai.ui;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Optional;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.widgets.Display;

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


    @Override
    public Optional<OutputStream> getOutputStream()
    {
        if (document != null)
        {
            return Optional.of(new DocumentOutputStream(document, getCharset()));
        }

        if (file != null)
        {
            return Optional.of(new FileOutputStream(file));
        }

        return Optional.empty();
    }

    private static class DocumentOutputStream
        extends OutputStream
    {
        private final IDocument document;
        private final Charset charset;
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        DocumentOutputStream(IDocument document, Charset charset)
        {
            this.document = document;
            this.charset = charset;
        }

        @Override
        public void write(int b) throws IOException
        {
            buffer.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException
        {
            buffer.write(b, off, len);
        }

        @Override
        public void close() throws IOException
        {
            byte[] bytes = buffer.toByteArray();
            final String text = new String(bytes, charset);
            Display.getDefault().asyncExec(() -> document.set(text));
            buffer.close();
        }
    }

    private static class FileOutputStream
        extends OutputStream
    {
        private final IFile file;
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        FileOutputStream(IFile file)
        {
            this.file = file;
        }

        @Override
        public void write(int b) throws IOException
        {
            buffer.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException
        {
            buffer.write(b, off, len);
        }

        @Override
        public void close() throws IOException
        {
            try
            {
                byte[] bytes = buffer.toByteArray();
                file.setContents(new ByteArrayInputStream(bytes), true, // force update
                    true, // keep history
                    null // no progress monitor
                );
            }
            catch (CoreException e)
            {
                throw new IOException(e);
            }
            finally
            {
                buffer.close();
            }
        }
    }
}
