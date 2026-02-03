/**
 *
 */
package com.e1c.edt.ai.ui;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.text.BadLocationException;

@SuppressWarnings("nls")
public class FileSystem implements IFileSystem
{
    @Override
    public IFile getProjectFile(IProject project, String relativePath)
    {
        // Check if the path is absolute
        if (relativePath != null && new File(relativePath).isAbsolute())
        {
            // For absolute paths, try to find the file in the workspace
            var absoluteFile = new File(relativePath);
            var workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
            var location = org.eclipse.core.runtime.Path.fromOSString(absoluteFile.getAbsolutePath());
            var foundFile = workspaceRoot.getFileForLocation(location);

            if (foundFile != null && foundFile.exists())
            {
                return foundFile;
            }
        }

        // For relative paths or if absolute path not found in workspace
        if (relativePath == null)
        {
            return null;
        }

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
        if (filePath == null || filePath.isBlank())
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

    @Override
    public String determineProjectName(String filePath)
    {
        if (filePath == null || filePath.isBlank())
        {
            return null;
        }

        var file = new File(filePath);
        boolean isRelativePath = !file.isAbsolute();

        var workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        var projects = workspaceRoot.getProjects();

        for (IProject project : projects)
        {
            if (!project.exists() || !project.isOpen())
            {
                continue;
            }

            var projectLocation = project.getLocation();
            if (projectLocation == null)
            {
                continue;
            }

            String projectLocationString = projectLocation.toOSString();

            if (isRelativePath)
            {
                // For relative paths, check if the file exists within the project
                var projectFile = new File(projectLocationString, normalizeFilePath(project, filePath));
                if (projectFile.exists())
                {
                    return project.getName();
                }
            }
            else
            {
                // For absolute paths, check if the file path starts with project location
                var absolutePath = file.getAbsolutePath();
                if (absolutePath.startsWith(projectLocationString))
                {
                    return project.getName();
                }
            }
        }

        return null;
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
}
