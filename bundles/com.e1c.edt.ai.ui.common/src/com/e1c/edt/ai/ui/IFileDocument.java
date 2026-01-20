/**
 *
 */
package com.e1c.edt.ai.ui;

import java.nio.charset.Charset;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;

import com.e1c.edt.ai.assistent.model.ProjectId;

public interface IFileDocument
{
    ProjectId getProjectId();

    Charset getCharset();

    IFile getFile();

    IDocument getDocument();

    void save() throws CoreException;
}
