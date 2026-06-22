/**
 *
 */
package com.e1c.edt.ai;

import java.io.File;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;

public class ProjectTools
    implements IProjectTools
{
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
            if (absolutePath.equals(projectLocationString)
                || absolutePath.startsWith(projectLocationString + File.separator))
            {
                return project.getName();
            }
        }

        return null;
    }

    @Override
    public Optional<IFile> getProjectFile(IProject project, String absolutePath)
    {
        if (absolutePath == null || absolutePath.isBlank())
        {
            return Optional.empty();
        }

        // A project-relative path (not absolute) is resolved directly against the project. This
        // matches the tool specs that accept "absolute or project-relative" paths; without it a
        // relative path would be resolved against the process CWD and never match the project.
        if (!new File(absolutePath).isAbsolute())
        {
            var relativePath = absolutePath.replace('\\', '/');
            if (relativePath.startsWith("/")) //$NON-NLS-1$
            {
                relativePath = relativePath.substring(1);
            }
            var projectPrefix = project.getName() + "/"; //$NON-NLS-1$
            if (relativePath.startsWith(projectPrefix))
            {
                relativePath = relativePath.substring(projectPrefix.length());
            }
            return Optional.of(project.getFile(new org.eclipse.core.runtime.Path(relativePath)));
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
            relativePath = ""; // Project root itself
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
            return Optional.of(project.getFile(new org.eclipse.core.runtime.Path(relativePath)));
        }

        return Optional.of(projectFile);
    }
}
