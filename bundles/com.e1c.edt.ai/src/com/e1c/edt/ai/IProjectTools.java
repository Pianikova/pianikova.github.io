/**
 *
 */
package com.e1c.edt.ai;

import java.util.Optional;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;

public interface IProjectTools
{
    String determineProjectName(String filePath);

    Optional<IFile> getProjectFile(IProject project, String relativePath);
}
