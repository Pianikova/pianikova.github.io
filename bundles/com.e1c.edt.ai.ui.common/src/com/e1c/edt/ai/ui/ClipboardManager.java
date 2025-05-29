/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;

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

@SuppressWarnings("nls")
public class ClipboardManager
    implements IClipboardManager, IClipboard, IExecutionListener
{
    private static final int MAX_SIZE = 8192;
    private static final HashSet<String> COPY_COMMAND_IDS = new HashSet<>();
    private static final HashSet<String> PASTE_COMMAND_IDS = new HashSet<>();
    private final IDispatcher dispatcher;
    private final IUISettings settings;
    private Optional<String> text = Optional.empty();
    private boolean isPasting;

    static
    {
        COPY_COMMAND_IDS.add("org.eclipse.ui.edit.copy");
        COPY_COMMAND_IDS.add("org.eclipse.xtend.ide.copyJavaCode");
        COPY_COMMAND_IDS.add("org.eclipse.jdt.ui.edit.text.java.raw.copy");

        PASTE_COMMAND_IDS.add("org.eclipse.ui.edit.paste");
        PASTE_COMMAND_IDS.add("org.eclipse.xtend.ide.pasteJavaCode");
        PASTE_COMMAND_IDS.add("org.eclipse.jdt.ui.edit.text.java.raw.paste");
    }

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
        var commandService = PlatformUI.getWorkbench().getService(ICommandService.class);
        commandService.addExecutionListener(this);
    }

    @Override
    public Optional<String> getText()
    {
        if (!settings.sendExtendedContext())
        {
            return Optional.empty();
        }

        var currentText = dispatcher.dispatch(() -> getTextFromClipoard()).flatMap(i -> i).orElse(null);
        var eclipseText = text.orElse(null);
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

    @Override
    public boolean isPasting()
    {
        return isPasting;
    }

    @Override
    public void preExecute(String commandId, ExecutionEvent event)
    {
        if (PASTE_COMMAND_IDS.contains(commandId))
        {
            isPasting = true;
        }
    }

    @Override
    public void postExecuteSuccess(String commandId, Object returnValue)
    {
        if (COPY_COMMAND_IDS.contains(commandId))
        {
            text = getTextFromClipoard();
        }

        if (PASTE_COMMAND_IDS.contains(commandId))
        {
            isPasting = false;
        }
    }

    @Override
    public void notHandled(String commandId, NotHandledException exception)
    {
        if (PASTE_COMMAND_IDS.contains(commandId))
        {
            isPasting = false;
        }
    }

    @Override
    public void postExecuteFailure(String commandId, ExecutionException exception)
    {
        if (PASTE_COMMAND_IDS.contains(commandId))
        {
            isPasting = false;
        }
    }

    private Optional<String> getTextFromClipoard()
    {
        var clipboard = new Clipboard(Display.getCurrent());
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
}
