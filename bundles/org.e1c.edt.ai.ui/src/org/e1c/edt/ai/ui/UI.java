/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.Optional;

import org.e1c.edt.ai.ILog;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension2;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.xtext.ui.editor.XtextEditor;

import com.google.inject.Inject;

public class UI
    implements IUI
{
    private ILog log;

    @Inject
    public UI(ILog log)
    {
        this.log = log;
    }

    @Override
    public Optional<IViewPart> showView(String viewId)
    {
        return getActivePage().map(activePage -> {
            try
            {
                return activePage.showView(viewId);
            }
            catch (PartInitException e)
            {
                log.logError(e);
            }

            return null;
        });
    }

    @Override
    public Optional<XtextEditor> getEditor()
    {
        return getEditorPart().map(editor -> editor.getAdapter(XtextEditor.class));
    }

    @Override
    public Optional<ITextViewer> getTextViewer()
    {
        return getTextOperationTarget().map(target -> {
            return (target instanceof ITextViewer) ? (ITextViewer)target : null;
        });
    }

    @Override
    public Optional<ITextViewerExtension2> getTextViewerExtension2()
    {
        return getTextOperationTarget().map(target -> {
            return (target instanceof ITextViewerExtension2) ? (ITextViewerExtension2)target : null;
        });
    }

    @Override
    public Optional<ISelectionProvider> getSelectionProvider()
    {
        return getEditor().map(editor -> editor.getSelectionProvider());
    }

    @Override
    public Optional<ITextSelection> getSelection()
    {
        return getSelectionProvider().map(selectionProvider -> {
            var selection = selectionProvider.getSelection();
            return selection instanceof ITextSelection ? (ITextSelection)selection : null;
        });
    }

    @Override
    public void select(ITextSelection selection)
    {
        getSelectionProvider().ifPresent(selectionProvider -> selectionProvider.setSelection(selection));
    }

    private Optional<IWorkbenchPage> getActivePage()
    {
        return Optional.ofNullable(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage());
    }

    private Optional<IEditorPart> getEditorPart()
    {
        return getActivePage().map(activePage -> activePage.getActiveEditor());
    }

    private Optional<ITextOperationTarget> getTextOperationTarget()
    {
        return getEditorPart().map(editor -> editor.getAdapter(ITextOperationTarget.class));
    }
}
