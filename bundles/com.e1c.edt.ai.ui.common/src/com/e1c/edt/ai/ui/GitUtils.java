/**
 *
 */
package com.e1c.edt.ai.ui;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

/**
 * Utility class for Git operations.
 */
public class GitUtils
{
    /**
     * Gets the Git repository for the given project.
     * @param project the Eclipse project
     * @return the Git repository, or null if the project is not in a Git repository
     */
    public static Repository getRepository(IProject project)
    {
        if (project == null || !project.exists())
        {
            return null;
        }

        try
        {
            // Try to find .git directory in the project location
            IPath projectLocation = project.getLocation();
            if (projectLocation == null)
            {
                return null;
            }

            File projectDir = projectLocation.toFile();
            
            // Look for .git directory
            File gitDir = findGitDirectory(projectDir);
            if (gitDir == null)
            {
                return null;
            }

            // Open the repository
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            builder.setGitDir(gitDir);
            builder.readEnvironment();
            builder.findGitDir();
            
            return builder.build();
        }
        catch (Exception e)
        {
            // Log error and return null
            return null;
        }
    }

    /**
     * Recursively searches for the .git directory.
     * @param directory the starting directory
     * @return the .git directory, or null if not found
     */
    private static File findGitDirectory(File directory)
    {
        if (directory == null)
        {
            return null;
        }

        File gitDir = new File(directory, ".git"); //$NON-NLS-1$
        if (gitDir.exists() && gitDir.isDirectory())
        {
            return gitDir;
        }

        // Check parent directory (for subdirectory checkouts)
        File parentDir = directory.getParentFile();
        if (parentDir != null)
        {
            gitDir = new File(parentDir, ".git"); //$NON-NLS-1$
            if (gitDir.exists() && gitDir.isDirectory())
            {
                return gitDir;
            }
        }

        return null;
    }
}