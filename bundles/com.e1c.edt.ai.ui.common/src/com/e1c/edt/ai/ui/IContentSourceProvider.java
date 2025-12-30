/**
 *
 */
package com.e1c.edt.ai.ui;

import java.util.Optional;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;

public interface IContentSourceProvider
{
    Optional<FileContent> getFileContent(IProject project, String relativePath);

    Optional<FileContent> getFileContent(IFile file);
}
