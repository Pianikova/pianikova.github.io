/**
 *
 */
package com.e1c.edt.ai.ui;

import java.nio.charset.Charset;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.texteditor.ITextEditor;

import com.e1c.edt.ai.assistent.model.ProjectId;

public interface IFileDocument
{
    ProjectId getProjectId();

    Charset getCharset();

    IFile getFile();

    IDocument getDocument();

    ITextEditor getTextEditor();

    void save() throws CoreException;

    void delete() throws CoreException;
}
