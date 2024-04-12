/*
 * Copyright (C) 2023, 1C
 */
package org.e1c.edt.ai.ui.handlers;

import org.e1c.edt.ai.ui.Activator;
import org.e1c.edt.ai.ui.ChatAPI;
import org.e1c.edt.ai.ui.ChatAPIProvider;
import org.e1c.edt.ai.ui.views.ChatView;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.xtext.ui.editor.XtextEditor;

/**
 * Class Handler of the Criticise the Code command.
 *
 * @author Bogdan Sushkov
 */
public class CriticiseAIHandler
    extends AbstractHandler
{
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        if (activePage != null)
        {
            IEditorPart editor = activePage.getActiveEditor();

            if (editor != null)
            {
                XtextEditor xtextEditor = editor.getAdapter(XtextEditor.class);

                ITextSelection textSelection = (ITextSelection)xtextEditor.getSelectionProvider().getSelection();
                try
                {
                    Display.getDefault().asyncExec(() -> {
                        ChatAPI chatAPI = ChatAPIProvider.getService();
                        chatAPI.reviewCode(textSelection.getText());
                    });
                    ChatView view = (ChatView)activePage.showView(ChatView.ID);
                    view.setFocus();
                }
                catch (PartInitException e)
                {
                    Activator.createErrorStatus(e.getMessage(), e);
                }

            }
        }
        return null;
    }

}
