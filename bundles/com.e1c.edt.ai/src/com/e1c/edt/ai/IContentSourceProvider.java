/**
 *
 */
package com.e1c.edt.ai;

import java.util.Optional;

import org.eclipse.core.resources.IFile;

public interface IContentSourceProvider
{
    Optional<IFileDocument> getFileDocument(IFile file);
}
