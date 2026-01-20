/**
 *
 */
package com.e1c.edt.ai.ui;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;

public interface IFileSystem
{
    IFile getProjectFile(IProject project, String relativePath);

    Iterable<String> getLines(IFileDocument fileDocument, int firstLineNumber, int linesNumber);

    boolean isPrintable(String text, double threshold);
}
