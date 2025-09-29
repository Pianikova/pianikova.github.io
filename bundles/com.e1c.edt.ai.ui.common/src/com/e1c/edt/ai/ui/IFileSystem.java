/**
 *
 */
package com.e1c.edt.ai.ui;

import java.io.InputStream;
import java.util.Optional;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

public interface IFileSystem
{
    Optional<String> getText(IContentReader contentReader);

    InputStream getContent(IFile file) throws CoreException;
}
