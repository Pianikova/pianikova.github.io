/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;

public class AIPartListener
    implements IPartListener2, ISelectionListener
{
    private final IUI ui;
    private final ICodeCompletion codeCompletion;
    private ITextViewer lastTextViewer;

    public AIPartListener(IUI ui, ICodeCompletion codeCompletion)
    {
        this.ui = ui;
        this.codeCompletion = codeCompletion;
    }

    @Override
    public void partOpened(IWorkbenchPartReference partRef)
    {
        partRef.getPage().addPostSelectionListener(this);
    }

    @Override
    public void partClosed(IWorkbenchPartReference partRef)
    {
        partRef.getPage().removePostSelectionListener(this);
    }

    @Override
    public void selectionChanged(IWorkbenchPart part, ISelection selection)
    {
        ui.getTextViewer().ifPresent(textViewer -> {
            if (lastTextViewer == textViewer)
            {
                return;
            }

            lastTextViewer = textViewer;
            codeCompletion.show(false);
        });
    }
}
