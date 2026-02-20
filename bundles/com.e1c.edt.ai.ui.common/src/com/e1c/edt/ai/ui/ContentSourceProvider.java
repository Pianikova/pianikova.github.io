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
import org.eclipse.ui.texteditor.ITextEditor;

import com.e1c.edt.ai.IContentSourceProvider;
import com.e1c.edt.ai.IFileDocument;
import com.e1c.edt.ai.ILog;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class ContentSourceProvider
    implements IContentSourceProvider
{
    private final ILog log;

    @Inject
    public ContentSourceProvider(ILog log)
    {
        Preconditions.checkNotNull(log);
        this.log = log;
    }

    @Override
    public Optional<IFileDocument> getFileDocument(IFile file)
    {
        if (file == null)
        {
            return Optional.empty();
        }

        IDocument document = null;
        ITextEditor textEditor = null;

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
                    var editorPart = editorRef.getEditor(false);
                    if (editorPart == null)
                    {
                        continue;
                    }

                    var input = editorPart.getEditorInput();
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

                    // Основное изменение здесь:
                    var textOperationTarget = editorPart.getAdapter(ITextOperationTarget.class);
                    if (textOperationTarget instanceof TextViewer)
                    {
                        var textViewer = (TextViewer)textOperationTarget;
                        document = textViewer.getDocument();
                        textEditor = editorPart.getAdapter(ITextEditor.class);
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
            return Optional.of(new FileDocument(document, textEditor, file, true));
        }

        if (file.exists())
        {
            try
            {
                var bytes = file.getContents().readAllBytes();
                var content = new String(bytes, file.getCharset());
                document = new Document(content);
                return Optional.of(new FileDocument(document, null, file, false));
            }
            catch (IOException | CoreException e)
            {
                return Optional.empty();
            }
        }

        return Optional.empty();
    }
}