/**
 *
 */
package com.e1c.edt.ai.ui;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.util.Optional;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

public class FileSystem implements IFileSystem
{
    @Override
    public Optional<String> getText(IContentReader contentReader)
    {
        byte[] bytes;
        try
        {
            bytes = contentReader.getInputStream().readAllBytes();
            var buffer = ByteBuffer.wrap(bytes);
            for (var charset : Charset.availableCharsets().values())
            {
                try
                {
                    var text = charset.newDecoder().decode(buffer).toString();
                    if (isPrintable(text, 85))
                    {
                        return Optional.of(text);
                    }
                }
                catch (CharacterCodingException e)
                {
                    continue;
                }
            }

        }
        catch (IOException | CoreException e)
        {
            //
        }

        return Optional.empty();
    }

    @Override
    public InputStream getContent(IFile file) throws CoreException
    {
        var filePath = file.getFullPath();
        try
        {
            FileBuffers.getTextFileBufferManager().connect(filePath, LocationKind.IFILE, null);
            var buffer =
                FileBuffers.getTextFileBufferManager().getTextFileBuffer(filePath, LocationKind.IFILE);
            if (buffer != null)
            {
                var doc = buffer.getDocument();
                String content = doc.get();
                try
                {
                    return new ByteArrayInputStream(content.getBytes(file.getCharset()));
                }
                catch (UnsupportedEncodingException | CoreException e)
                {
                    //
                }
            }
        }
        finally
        {
            FileBuffers.getTextFileBufferManager().disconnect(filePath, LocationKind.IFILE, null);
        }

        return file.getContents();
    }

    private static boolean isPrintable(String text, double threshold)
    {
        int printable = 0;
        for (int i = 0; i < text.length(); i++)
        {
            if (isPrintable(text.charAt(i)))
            {
                printable++;
            }
        }

        return ((double)100) * printable / text.length() >= threshold;
    }

    private static boolean isPrintable(char c)
    {
        var block = Character.UnicodeBlock.of(c);
        return (!Character.isISOControl(c)) && block != null && block != Character.UnicodeBlock.SPECIALS;
    }
}
