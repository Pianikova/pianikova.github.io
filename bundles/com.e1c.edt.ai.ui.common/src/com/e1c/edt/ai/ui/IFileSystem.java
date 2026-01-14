/**
 *
 */
package com.e1c.edt.ai.ui;

import java.util.Optional;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;

public interface IFileSystem
{
    IFile getProjectFile(IProject project, String relativePath);

    Optional<String> getText(IFileContent contentReader, int firstLineNumber, int linesNumber);
}
