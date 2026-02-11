/**
 *
 */
package com.e1c.edt.ai.ui;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.BadLocationException;

@SuppressWarnings("nls")
public class FileSystem implements IFileSystem
{
    @Override
    public Optional<IFile> getProjectFile(IProject project, String absolutePath)
    {
        if (absolutePath == null || absolutePath.isBlank())
        {
            return Optional.empty();
        }

        // Get project location
        var projectLocation = project.getLocation();
        if (projectLocation == null)
        {
            return Optional.empty();
        }

        // Normalize both paths to the same form
        var absolutePathFile = new File(absolutePath).getAbsoluteFile();
        var projectLocationFile = new File(projectLocation.toOSString()).getAbsoluteFile();
        var projectLocationString = projectLocationFile.getAbsolutePath();

        // Check that the path is actually inside the project
        var absolutePathString = absolutePathFile.getAbsolutePath();
        if (!absolutePathString.startsWith(projectLocationString))
        {
            return Optional.empty();
        }

        // Calculate relative path inside the project
        String relativePath;
        if (absolutePathString.equals(projectLocationString))
        {
            relativePath = "";  // Project root itself
        }
        else
        {
            relativePath = absolutePathString.substring(projectLocationString.length());
            // Remove leading separator if present
            if (relativePath.startsWith(File.separator))
            {
                relativePath = relativePath.substring(File.separator.length());
            }
        }

        // Convert backslashes to forward slashes for Eclipse API compatibility
        relativePath = relativePath.replace(File.separatorChar, '/');
        var projectFile = project.getFile(relativePath);

        // Check if file exists in Eclipse workspace before returning
        if (projectFile == null || !projectFile.exists())
        {
            return Optional.empty();
        }

        return Optional.of(projectFile);
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

        var workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        var location = Path.fromOSString(filePath);

        // First try using native Eclipse API
        var container = workspaceRoot.getContainerForLocation(location);

        if (container != null)
        {
            var project = container.getProject();
            if (project != null && project.exists() && project.isOpen())
            {
                return project.getName();
            }
        }

        // Fallback: if API didn't work, use exact path comparison
        var absolutePath = new File(filePath).getAbsolutePath();

        // Sort projects by path length (longest to shortest)
        // to avoid conflicts with nested projects
        var projects = Arrays.stream(workspaceRoot.getProjects())
            .filter(p -> p.exists() && p.isOpen())
            .filter(p -> p.getLocation() != null)
            .sorted((p1, p2) -> {
                var len1 = p1.getLocation().toOSString().length();
                var len2 = p2.getLocation().toOSString().length();
                return len2 - len1; // Descending: longest paths first
            })
            .collect(Collectors.toList());

        for (IProject project : projects)
        {
            var projectLocationString = project.getLocation().toOSString();

            // Exact check with path separator
            if (absolutePath.equals(projectLocationString) ||
                absolutePath.startsWith(projectLocationString + File.separator))
            {
                return project.getName();
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
