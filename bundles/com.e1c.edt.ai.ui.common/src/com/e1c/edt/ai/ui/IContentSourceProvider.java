/**
 * 
 */
package com.e1c.edt.ai.ui;

import java.util.Optional;

import org.eclipse.core.resources.IProject;

/**
 * @author Pyanikov_N
 *
 */
public interface IContentSourceProvider
{

    Optional<FileContent> getFileContent(IProject project, String relativePath);

}
