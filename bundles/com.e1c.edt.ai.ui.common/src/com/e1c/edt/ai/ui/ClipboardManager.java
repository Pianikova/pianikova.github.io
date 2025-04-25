/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.util.Objects;
import java.util.Optional;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IExecutionListener;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;

import com.e1c.edt.ai.IUISettings;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class ClipboardManager
    implements IClipboardManager, IClipboard
{
    private static final int MAX_SIZE = 8192;
    private final IDispatcher dispatcher;
    private final IUISettings settings;
    private final CopyExecutionListener copyCommandListener = new CopyExecutionListener();

    @Inject
    public ClipboardManager(IDispatcher dispatcher, IUISettings settings)
    {
        Preconditions.checkNotNull(dispatcher);
        Preconditions.checkNotNull(settings);
        this.dispatcher = dispatcher;
        this.settings = settings;
    }

    @Override
    public void initialize()
    {
        var copyCommand = getCopyCommand();
        copyCommand.removeExecutionListener(copyCommandListener);
        copyCommand.addExecutionListener(copyCommandListener);
    }

    @Override
    public Optional<String> getText()
    {
        if (!settings.sendExtendedContext())
        {
            return Optional.empty();
        }

        var currentText = dispatcher.dispatch(() -> getTextFromClipoard()).flatMap(i -> i).orElse(null);
        var eclipseText = copyCommandListener.Text.orElse(null);
        if (!Objects.equals(currentText, eclipseText))
        {
            return Optional.empty();
        }

        if (eclipseText == null || eclipseText.isBlank())
        {
            return Optional.empty();
        }

        if (eclipseText.length() > MAX_SIZE)
        {
            eclipseText = eclipseText.substring(0, MAX_SIZE);
        }

        return Optional.of(eclipseText);
    }

    private Optional<String> getTextFromClipoard()
    {
        var clipboard = getClipboard();
        try
        {
            var val = clipboard.getContents(TextTransfer.getInstance());
            if (val instanceof String)
            {
                var text = (String)val;
                if (text != null && !text.isBlank())
                {
                    return Optional.of(text);
                }
            }

            return Optional.empty();
        }
        finally
        {
            clipboard.dispose();
        }
    }

    private Clipboard getClipboard()
    {
        return new Clipboard(Display.getCurrent());
    }

    private Command getCopyCommand()
    {
        ICommandService commandService = PlatformUI.getWorkbench().getService(ICommandService.class);
        return commandService.getCommand("org.eclipse.ui.edit.copy"); //$NON-NLS-1$
    }

    private class CopyExecutionListener
        implements IExecutionListener
    {
        public Optional<String> Text = Optional.empty();

        @Override
        public void preExecute(String commandId, ExecutionEvent event)
        {
            //
        }

        @Override
        public void postExecuteSuccess(String commandId, Object returnValue)
        {
            Text = getTextFromClipoard();
        }

        @Override
        public void notHandled(String commandId, NotHandledException exception)
        {
            //
        }

        @Override
        public void postExecuteFailure(String commandId, ExecutionException exception)
        {
            //
        }
    }
}
