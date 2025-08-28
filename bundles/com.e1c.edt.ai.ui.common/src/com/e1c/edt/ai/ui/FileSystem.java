/**
 *
 */
package com.e1c.edt.ai.ui;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class FileSystem implements IFileSystem
{
    @Override
    public Optional<String> getText(Path filePath)
    {
        for (var charset : Charset.availableCharsets().values())
        {
            try
            {
                return Optional.ofNullable(Files.readString(filePath, charset));
            }
            catch (IOException error)
            {
                //
            }
        }

        return Optional.empty();
    }

    @Override
    public boolean isTextFile(Path filePath)
    {
        try
        {
            // MIME-type check
            var mimeType = Files.probeContentType(filePath);
            if (mimeType != null && mimeType.startsWith("text/")) //$NON-NLS-1$
            {
                return true;
            }

            // Content check
            return isLikelyTextContent(filePath);
        }
        catch (IOException e)
        {
            return false;
        }
    }

    private static boolean isLikelyTextContent(Path filePath) throws IOException
    {
        try (var stream = new BufferedInputStream(new FileInputStream(filePath.toString())))
        {
            var maxBytes = 4096;
            var buffer = new byte[maxBytes];
            var length = stream.read(buffer);
            var textChars = 0;
            for (int i = 0; i < length; i++)
            {
                byte b = buffer[i];
                // Allowed characters: printable ASCII + control characters
                if (b >= 0x09 && b <= 0x0D)
                    continue; // \t\n\r etc.
                if (b >= 0x20 && b <= 0x7E)
                {
                    // Printable characters
                    textChars++;
                    continue;
                }
                // UTF-8 Multi-Byte Characters (start)
                if ((b & 0xE0) == 0xC0 && i + 1 < length)
                {
                    i += 1;
                }
                else
                {
                    if ((b & 0xF0) == 0xE0 && i + 2 < length)
                    {
                        i += 2;
                    }
                    else
                    {
                        if ((b & 0xF8) == 0xF0 && i + 3 < length)
                        {
                            i += 3;
                        }
                        else
                        {
                            return false;
                        }
                    }
                }
            }

            return (textChars > length * 0.7); // 70% of characters are printable
        }
    }
}
