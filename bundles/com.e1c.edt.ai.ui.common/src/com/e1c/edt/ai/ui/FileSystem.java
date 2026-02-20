/**
 *
 */
package com.e1c.edt.ai.ui;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.eclipse.jface.text.BadLocationException;

import com.e1c.edt.ai.IFileDocument;

@SuppressWarnings("nls")
public class FileSystem implements IFileSystem
{
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

    @Override
    public boolean fileExists(String filePath) throws IOException
    {
        if (filePath == null || filePath.isBlank())
        {
            return false;
        }

        return Files.exists(Paths.get(filePath));
    }

    @Override
    public boolean isFileEmpty(String filePath) throws IOException
    {
        if (filePath == null || filePath.isBlank())
        {
            return false;
        }

        var path = Paths.get(filePath);
        if (!Files.exists(path))
        {
            return false;
        }

        return Files.size(path) == 0;
    }

    @Override
    public byte[] readAllBytes(String filePath) throws IOException
    {
        if (filePath == null || filePath.isBlank())
        {
            throw new IOException("File path cannot be null or empty");
        }

        return Files.readAllBytes(Paths.get(filePath));
    }

    @Override
    public void writeAllBytes(String filePath, byte[] data) throws IOException
    {
        if (filePath == null || filePath.isBlank())
        {
            throw new IOException("File path cannot be null or empty");
        }

        var path = Paths.get(filePath);
        var parent = path.getParent();

        if (parent != null && !Files.exists(parent))
        {
            Files.createDirectories(parent);
        }

        Files.write(path, data);
    }

    @Override
    public void deleteFile(String filePath) throws IOException
    {
        if (filePath == null || filePath.isBlank())
        {
            throw new IOException("File path cannot be null or empty");
        }

        Files.deleteIfExists(Paths.get(filePath));
    }

    @Override
    public Iterable<String> getLines(Reader reader)
    {
        return () -> new BufferedReaderLineIterator(reader);
    }

    /**
     * Iterator that reads lines from a BufferedReader on-demand, preserving original line separators.
     * Handles BOM (Byte Order Mark) at the beginning of the file.
     */
    private static class BufferedReaderLineIterator
        implements java.util.Iterator<String>
    {
        private final Reader reader;
        private String nextLine = null;
        private boolean bomSkipped = false;
        private boolean finished = false;

        public BufferedReaderLineIterator(Reader reader)
        {
            this.reader = reader;
        }

        @Override
        public boolean hasNext()
        {
            if (finished)
            {
                return false;
            }

            nextLine = readNextLine();
            return nextLine != null;
        }

        @Override
        public String next()
        {
            return nextLine;
        }

        private String readNextLine()
        {
            StringBuilder currentLine = new StringBuilder();
            try
            {
                int ch;
                while ((ch = reader.read()) != -1)
                {
                    if (ch == '\r')
                    {
                        // Check if next char is \n (Windows-style CRLF)
                        reader.mark(1);
                        int nextCh = reader.read();
                        if (nextCh == '\n')
                        {
                            // Line ending is CRLF
                            String lineContent = currentLine.toString();
                            if (!bomSkipped)
                            {
                                lineContent = removeBOM(lineContent);
                                bomSkipped = true;
                            }

                            currentLine.setLength(0);
                            return lineContent + "\r\n"; //$NON-NLS-1$
                        }
                        else
                        {
                            // Line ending is CR only (old Mac style)
                            // Reset reader since we already read the next character
                            if (nextCh != -1)
                            {
                                reader.reset();
                            }

                            String lineContent = currentLine.toString();
                            if (!bomSkipped)
                            {
                                lineContent = removeBOM(lineContent);
                                bomSkipped = true;
                            }

                            currentLine.setLength(0);
                            return lineContent + "\r"; //$NON-NLS-1$
                        }
                    }
                    else if (ch == '\n')
                    {
                        // Line ending is LF (Unix-style)
                        String lineContent = currentLine.toString();
                        if (!bomSkipped)
                        {
                            lineContent = removeBOM(lineContent);
                            bomSkipped = true;
                        }

                        currentLine.setLength(0);
                        return lineContent + "\n"; //$NON-NLS-1$
                    }
                    else
                    {
                        currentLine.append((char)ch);
                    }
                }

                // Handle the last line if it doesn't have a line ending
                if (currentLine.length() > 0)
                {
                    String lineContent = currentLine.toString();
                    if (!bomSkipped)
                    {
                        lineContent = removeBOM(lineContent);
                        bomSkipped = true;
                    }

                    finished = true;
                    return lineContent;
                }

                finished = true;
                return null;
            }
            catch (IOException e)
            {
                throw new RuntimeException("Error reading line", e); //$NON-NLS-1$
            }
        }

        /**
         * Removes BOM (Byte Order Mark) from the beginning of the content if present.
         *
         * @param content the content to process
         * @return the content without BOM
         */
        private static String removeBOM(String content)
        {
            if (content == null || content.isEmpty())
            {
                return content;
            }

            // UTF-8 BOM is EF BB BF
            if (content.startsWith("\uFEFF")) //$NON-NLS-1$
            {
                return content.substring(1);
            }

            return content;
        }
    }
}
