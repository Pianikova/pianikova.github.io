/*
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Class Handler of the Explain the Code command.
 * <b> IN DEVELOPMENT </b>
 * @author Bogdan Sushkov
 */
public class ExplainAIHandler
    extends AbstractHandler
{
    private static final String LABEL = "Объяснить код"; //$NON-NLS-1$
    private static final String MESSAGE = "Команда выполнена"; //$NON-NLS-1$

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
        MessageDialog.openInformation(window.getShell(), LABEL, MESSAGE);
        return null;
    }
}
