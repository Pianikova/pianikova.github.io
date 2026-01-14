/**
 *
 */
package com.e1c.edt.ai.ui;

import java.util.Optional;

import org.eclipse.core.resources.IFile;

public interface IContentSourceProvider
{
    Optional<FileContent> getFileContent(IFile file);
}
