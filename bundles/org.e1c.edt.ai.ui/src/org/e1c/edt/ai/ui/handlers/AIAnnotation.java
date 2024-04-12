/*
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui.handlers;

import java.util.function.Consumer;

import org.e1c.edt.ai.ui.Activator;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.inlined.LineContentAnnotation;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.xtext.ui.editor.XtextEditor;
/**
 * @author Bogdan Sushkov
 *
 */
public class AIAnnotation
    extends LineContentAnnotation
{

    private String text;
    private Position position;
    private ISourceViewer viewer;
    /**
     * @param position
     * @param viewer
     */
    public AIAnnotation(Position position, ISourceViewer viewer)
    {
        super(position, viewer);
        this.position = position;
        this.viewer = viewer;
    }

    @Override
    public void setText(String text)
    {
        super.setText(text);
        this.text = text;
    }

    @Override
    public Consumer<MouseEvent> getAction(MouseEvent e)
    {
        return (Consumer<MouseEvent>)action -> {
            Display.getDefault().asyncExec(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        viewer.getDocument().replace(position.getOffset(), 0, text);
                        setCursorOffset(position.getOffset());
                    }
                    catch (BadLocationException e)
                    {
                        Activator.logError(e);
                    }
                }
            });
        };
    }

    private void setCursorOffset(int offset)
    {
        try
        {
            IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
            if (activePage != null)
            {
                IEditorPart editor = activePage.getActiveEditor();

                if (editor != null)
                {
                    XtextEditor xtextEditor = editor.getAdapter(XtextEditor.class);
                    ISelectionProvider selectionProvider = xtextEditor.getSelectionProvider();
                    if (selectionProvider != null) {
                        selectionProvider.setSelection(new TextSelection(offset, 0));
                    }
                }
            }
        }
        catch (Exception e)
        {
            Activator.logError(e);
        }
    }
}
