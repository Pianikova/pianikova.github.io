/**
 *
 */
package com.e1c.edt.ai;

import java.nio.charset.Charset;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.core.resources.IProject;

public interface IFileDocument
{
    IProject getProject();

    Charset getCharset();

    IFile getFile();

    IDocument getDocument();

    ITextEditor getTextEditor();

    /**
     * Tells whether the document carries changes that are not yet saved to disk.
     *
     * @return {@code true} when the underlying editor buffer is dirty, {@code false} otherwise
     */
    default boolean isDirty()
    {
        return false;
    }

    void setContent(String content);

    /**
     * Replaces a single contiguous region of the document instead of resetting its whole content.
     * Keeps Xtext's re-lexing bounded to the changed span, avoiding a full-document re-tokenization
     * on the UI thread.
     *
     * @param offset start offset of the region to replace
     * @param length length of the region to replace
     * @param text the replacement text
     * @throws BadLocationException if the region lies outside the document bounds
     */
    void replaceRegion(int offset, int length, String text) throws BadLocationException;

    void save() throws CoreException;

    void delete() throws CoreException;
}
