/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.Optional;

import org.e1c.edt.ai.ILog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.xtext.ui.editor.XtextEditor;

public class UI
    implements IUI
{
    private ILog log;

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
        return getActivePage().map(activePage -> activePage.getActiveEditor())
            .map(editor -> editor.getAdapter(XtextEditor.class));
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

    public Optional<ISelectionProvider> getSelectionProvider()
    {
        return getEditor().map(editor -> editor.getSelectionProvider());
    }
}
