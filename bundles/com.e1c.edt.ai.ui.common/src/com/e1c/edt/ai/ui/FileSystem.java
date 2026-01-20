/**
 *
 */
package com.e1c.edt.ai.ui;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.text.BadLocationException;

public class FileSystem implements IFileSystem
{
    @Override
    public IFile getProjectFile(IProject project, String relativePath)
    {
        var filePath = normalizeFilePath(project, relativePath);
        var file = project.getFile(filePath);
        return file;
    }

    @Override
    public Iterable<String> getLines(IFileDocument fileDocument, int firstLineNumber, int linesNumber)
    {
        return () -> new LineIterator(fileDocument, firstLineNumber, linesNumber);
    }

    @Override
    public boolean isPrintable(String text, double threshold)
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

    private static class LineIterator
        implements Iterator<String>
    {
        private final IFileDocument fileDocument;
        private final int endLine;
        private int currentLine;

        public LineIterator(IFileDocument fileDocument, int firstLineNumber, int linesNumber)
        {
            this.fileDocument = fileDocument;
            var doc = fileDocument.getDocument();
            int totalLines = doc.getNumberOfLines();
            this.currentLine = firstLineNumber;
            this.endLine = Math.min(firstLineNumber + linesNumber, totalLines);
        }

        @Override
        public boolean hasNext()
        {
            return currentLine < endLine;
        }

        @Override
        public String next()
        {
            if (!hasNext())
            {
                throw new NoSuchElementException();
            }
            try
            {
                var doc = fileDocument.getDocument();
                var line = doc.get(doc.getLineOffset(currentLine), doc.getLineLength(currentLine));
                currentLine++;
                return line;
            }
            catch (BadLocationException e)
            {
                throw new RuntimeException("Error reading line: " + currentLine, e); //$NON-NLS-1$
            }
        }
    }
}
