package com.e1c.edt.ai.ui;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import com.e1c.edt.ai.IFileDocument;
import com.e1c.edt.ai.assistent.model.ProjectId;
import com.google.common.base.Preconditions;

public class FileDocument
    implements IFileDocument
{
    private final IDocument document;
    private final ITextEditor textEditor;
    private final IFile file;
    private final boolean isOpened;
    private int savedCursorOffset = -1;
    private int savedSelectionLength = 0;

    public FileDocument(IDocument document, ITextEditor textEditor, IFile file, boolean isOpened)
    {
        Preconditions.checkNotNull(document);
        Preconditions.checkNotNull(file);

        this.document = document;
        this.textEditor = textEditor;
        this.file = file;
        this.isOpened = isOpened;
    }

    @Override
    public ProjectId getProjectId()
    {
        return new ProjectId(file.getProject());
    }

    @Override
    public Charset getCharset()
    {
        try
        {
            return Charset.forName(file.getCharset());
        }
        catch (CoreException e)
        {
            return Charset.forName(ResourcesPlugin.getEncoding());
        }
    }

    @Override
    public IFile getFile()
    {
        return file;
    }

    @Override
    public IDocument getDocument()
    {
        return document;
    }

    @Override
    public ITextEditor getTextEditor()
    {
        return textEditor;
    }

    @Override
    public void save() throws CoreException
    {
        if (isOpened)
        {
            var textEditor = getTextEditor();
            if (textEditor != null)
            {
                saveCursorPosition();
                saveThroughEditor(textEditor);
                restoreCursorPosition();
                return;
            }
        }

        saveDirectly();
    }

    public void setContent(String content)
    {
        // Validate input
        if (content == null)
        {
            content = ""; //$NON-NLS-1$
        }

        // Save cursor position before modifying document
        saveCursorPosition();

        try
        {
            document.set(content);
        }
        finally
        {
            // Always try to restore cursor position, even if set() failed
            restoreCursorPosition();
        }
    }

    private void saveThroughEditor(ITextEditor textEditor) throws CoreException
    {
        textEditor.doSave(new NullProgressMonitor());
    }

    private void saveDirectly() throws CoreException
    {
        var content = document.get();
        var bytes = content.getBytes(getCharset());
        file.setContents(new ByteArrayInputStream(bytes), true, true, null);
    }

    @Override
    public void delete() throws CoreException
    {
        if (isOpened)
        {
            var textEditor = getTextEditor();
            if (textEditor != null)
            {
                deleteThroughEditor(textEditor);
                return;
            }
        }

        deleteDirectly();
    }

    private void deleteThroughEditor(ITextEditor textEditor) throws CoreException
    {
        textEditor.close(false);
        file.delete(true, new NullProgressMonitor());
    }

    private void deleteDirectly() throws CoreException
    {
        file.delete(true, new NullProgressMonitor());
    }

    private void saveCursorPosition()
    {
        if (textEditor != null)
        {
            try
            {
                ISelectionProvider selectionProvider = textEditor.getSelectionProvider();
                if (selectionProvider != null)
                {
                    var selection = selectionProvider.getSelection();
                    if (selection instanceof ITextSelection)
                    {
                        ITextSelection textSelection = (ITextSelection)selection;
                        savedCursorOffset = textSelection.getOffset();
                        savedSelectionLength = textSelection.getLength();
                    }
                }
            }
            catch (Exception e)
            {
                // Ignore cursor save errors - use default values
                savedCursorOffset = -1;
                savedSelectionLength = 0;
            }
        }
    }

    private void restoreCursorPosition()
    {
        if (textEditor != null && savedCursorOffset >= 0)
        {
            try
            {
                var selectionProvider = textEditor.getSelectionProvider();
                if (selectionProvider == null)
                {
                    return;
                }

                IDocument currentDocument =
                    textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());
                if (currentDocument == null)
                {
                    return;
                }

                // Ensure the cursor position is within document bounds
                var documentLength = currentDocument.getLength();
                var cursorOffset = Math.min(savedCursorOffset, Math.max(0, documentLength));
                var selectionLength = Math.min(savedSelectionLength, Math.max(0, documentLength - cursorOffset));

                var newSelection =
                    new org.eclipse.jface.text.TextSelection(currentDocument, cursorOffset, selectionLength);
                selectionProvider.setSelection(newSelection);
            }
            catch (Exception e)
            {
                // Ignore cursor restoration errors - content is more important
            }
        }
    }
}
