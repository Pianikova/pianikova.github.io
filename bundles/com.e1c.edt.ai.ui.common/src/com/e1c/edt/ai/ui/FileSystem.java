/**
 *
 */
package com.e1c.edt.ai.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Optional;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;

import com.e1c.edt.ai.ILog;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class FileSystem implements IFileSystem
{
    private final ILog log;

    @Inject
    public FileSystem(ILog log)
    {
        Preconditions.checkNotNull(log);
        this.log = log;
    }

    @Override
    public IFile getProjectFile(IProject project, String relativePath)
    {
        var filePath = normalizeFilePath(project, relativePath);
        var file = project.getFile(filePath);
        return file;
    }

    @Override
    public Optional<String> getText(IFileContent contentReader, int firstLineNumber, int linesNumber)
    {
        var optionalInpiutStream = contentReader.getInputStream();
        try (var is = optionalInpiutStream.get();
            var isr = new InputStreamReader(is, contentReader.getCharset());
            var reader = new BufferedReader(isr))
        {
            var targetEndLine = firstLineNumber + linesNumber - 1;
            var currentLine = 0;
            int c;
            var resultContent = new StringBuilder();
            while ((c = reader.read()) != -1)
            {
                // Check if current line is within target range
                var inTarget = (currentLine >= firstLineNumber && currentLine <= targetEndLine);
                if (c == '\r')
                {
                    // Handle carriage return (possible Windows line ending)
                    reader.mark(1);
                    var next = reader.read();
                    if (next == '\n')
                    {
                        // CRLF sequence (Windows)
                        if (inTarget)
                        {
                            resultContent.append((char)c);
                            resultContent.append((char)next);
                        }

                        currentLine++;
                        if (currentLine > targetEndLine)
                            break;
                    }
                    else
                    {
                        // Single CR (Mac/old systems)
                        if (next != -1)
                            reader.reset(); // Put back non-LF character
                        if (inTarget)
                        {
                            resultContent.append((char)c);
                        }

                        currentLine++;
                        if (currentLine > targetEndLine)
                        {
                            break;
                        }
                    }
                }
                else if (c == '\n')
                {
                    // LF sequence (Unix/Linux)
                    if (inTarget)
                    {
                        resultContent.append((char)c);
                    }

                    currentLine++;
                    if (currentLine > targetEndLine)
                    {
                        break;
                    }
                }
                else
                {
                    // Regular character
                    if (inTarget)
                    {
                        resultContent.append((char)c);
                    }
                }
            }

            var text = resultContent.toString();
            var isPrintable = isPrintable(text, 85);
            if (!isPrintable)
            {
                return Optional.empty();
            }

            return Optional.of(text);
        }
        catch (IOException e)
        {
            return Optional.empty();
        }
    }

    private static boolean isPrintable(String text, double threshold)
    {
        if (text == null || text.isBlank())
        {
            return true;
        }

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

    @SuppressWarnings("nls")
    private static String normalizeFilePath(IProject project, String filePath)
    {
        if (filePath == null | filePath.isBlank())
        {
            return filePath;
        }

        if (!filePath.startsWith("/"))
        {
            filePath = "/" + filePath;
        }

        var projectName = project.getName();
        if (filePath.startsWith("/" + projectName))
        {
            filePath = filePath.substring(1 + projectName.length());
        }

        return filePath;
    }
}
