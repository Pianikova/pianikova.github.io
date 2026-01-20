/**
 *
 */
package com.e1c.edt.ai.ui;

import java.io.IOException;
import java.util.Optional;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.Document;
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
    public Optional<FileDocument> getFileDocument(IFile file)
    {
        if (file == null)
        {
            return Optional.empty();
        }

        IDocument document = null;
        for (var window : PlatformUI.getWorkbench().getWorkbenchWindows())
        {
            var page = window.getActivePage();
            if (page == null)
            {
                continue;
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
                        document = ((TextViewer)textOperationTarget).getDocument();
                        break;
                    }
                }
                catch (Exception error)
                {
                    log.logError(error);
                }
            }

            if (document != null)
            {
                break;
            }
        }

        if (document != null)
        {
            return Optional.of(new FileDocument(document, file, true));
        }

        if (file.exists())
        {
            try
            {
                var bytes = file.getContents().readAllBytes();
                var content = new String(bytes, file.getCharset());
                document = new Document(content);
                return Optional.of(new FileDocument(document, file, false));
            }
            catch (IOException | CoreException e)
            {
                return Optional.empty();
            }
        }

        return Optional.empty();
    }
}
