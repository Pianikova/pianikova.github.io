/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.ui.quickfix;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.handlers.IHandlerService;

import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.ui.BaseActivator;
import com.e1c.edt.ai.ui.Messages;
import com.google.inject.Inject;

/** Ctrl+1 proposal list for the EDT Configuration Problems view. */
public class ConfigurationProblemQuickAssistHandler
    extends AbstractHandler
{
    private static final String EDT_QUICK_FIX_COMMAND_ID = "com._1c.g5.v8.dt.ui.command.quickFix"; //$NON-NLS-1$

    @Inject
    ILog log;

    public ConfigurationProblemQuickAssistHandler()
    {
        BaseActivator.injectMembers(this);
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        try
        {
            ICommandService commandService = HandlerUtil.getActiveWorkbenchWindow(event).getService(ICommandService.class);
            IHandlerService handlerService = HandlerUtil.getActiveWorkbenchWindow(event).getService(IHandlerService.class);
            List<QuickFixCommand> commands = new ArrayList<>();
            addIfEnabled(commands, commandService.getCommand(ConfigurationProblemAIHandler.COMMAND_ID));
            addIfEnabled(commands, commandService.getCommand(EDT_QUICK_FIX_COMMAND_ID));
            if (commands.isEmpty())
            {
                return null;
            }

            ElementListSelectionDialog dialog = new ElementListSelectionDialog(HandlerUtil.getActiveShell(event),
                new LabelProvider());
            dialog.setTitle(Messages.ConfigurationProblemQuickFixDialogTitle);
            dialog.setMessage(Messages.ConfigurationProblemQuickFixDialogMessage);
            dialog.setElements(commands.toArray());
            if (dialog.open() == Window.OK && dialog.getFirstResult() instanceof QuickFixCommand)
            {
                handlerService.executeCommand(((QuickFixCommand)dialog.getFirstResult()).id, null);
            }
        }
        catch (Exception e)
        {
            log.logError(e);
        }
        return null;
    }

    private static void addIfEnabled(List<QuickFixCommand> result, Command command)
    {
        if (command != null && command.isDefined() && command.isEnabled())
        {
            try
            {
                result.add(new QuickFixCommand(command.getId(), command.getName()));
            }
            catch (Exception e)
            {
                // A defined command normally has a name; omit a malformed contribution.
            }
        }
    }

    private static final class QuickFixCommand
    {
        private final String id;
        private final String label;

        QuickFixCommand(String id, String label)
        {
            this.id = id;
            this.label = label;
        }

        @Override
        public String toString()
        {
            return label;
        }
    }
}
