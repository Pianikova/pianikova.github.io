/**
 *
 */
package com.e1c.edt.ai.ui;

import java.util.Optional;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.ui.PlatformUI;

import com.e1c.edt.ai.ILog;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class ContentSourceProvider implements IContentSourceProvider
{
    private final ILog log;

    @Inject
    public ContentSourceProvider(ILog log)
    {
        Preconditions.checkNotNull(log);
        this.log = log;
    }

    @Override
    public Optional<FileContent> getFileContent(IFile file)
    {
        if (file == null)
        {
            return Optional.empty();
        }

        var document = getContentFromOpenEditorDocument(file);
        if (document != null || file.exists())
        {
            return Optional.of(new FileContent(document, file));
        }

        return Optional.empty();
    }

    private IDocument getContentFromOpenEditorDocument(IFile file)
    {
        for (var window : PlatformUI.getWorkbench().getWorkbenchWindows())
        {
            var page = window.getActivePage();
            if (page == null)
            {
                return null;
            }

            for (var editorRef : page.getEditorReferences())
            {
                try
                {
                    var editor = editorRef.getEditor(false);
                    if (editor == null)
                    {
                        continue;
                    }

                    var input = editor.getEditorInput();
                    if (input == null)
                    {
                        continue;
                    }

                    var editorFile = input.getAdapter(IFile.class);
                    if (editorFile == null)
                    {
                        continue;
                    }

                    if (!file.equals(editorFile))
                    {
                        continue;
                    }

                    var textOperationTarget = editor.getAdapter(ITextOperationTarget.class);
                    if (textOperationTarget instanceof TextViewer)
                    {
                        return ((TextViewer)textOperationTarget).getDocument();
                    }
                }
                catch (Exception error)
                {
                    log.logError(error);
                }
            }
        }

        return null;
    }
}
