/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IExecutionListener;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;

import com.e1c.edt.ai.IClock;
import com.e1c.edt.ai.assistent.model.ClipboardInfo;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

@SuppressWarnings("nls")
public class ClipboardManager
    implements IInitializable, IClipboard, IExecutionListener, Listener
{
    private static final int MAX_SIZE = 8192;
    private static final Duration MAX_DURATION = Duration.ofMinutes(15);
    private static final HashSet<String> COPY_COMMAND_IDS = new HashSet<>();
    private static final HashSet<String> PASTE_COMMAND_IDS = new HashSet<>();
    private final IDispatcher dispatcher;
    private final IClock clock;
    private Optional<String> text = Optional.empty();
    private Optional<IFile> file = Optional.empty();
    private Optional<IFile> currentFile = Optional.empty();
    private LocalDateTime expirationDate;
    private boolean isPasting;

    static
    {
        COPY_COMMAND_IDS.add("org.eclipse.ui.edit.copy");
        COPY_COMMAND_IDS.add("org.eclipse.xtend.ide.copyJavaCode");
        COPY_COMMAND_IDS.add("org.eclipse.jdt.ui.edit.text.java.raw.copy");
        COPY_COMMAND_IDS.add("org.eclipse.ui.edit.cut");
        COPY_COMMAND_IDS.add("org.eclipse.xtend.ide.cutJavaCode");
        COPY_COMMAND_IDS.add("org.eclipse.jdt.ui.edit.text.java.raw.cut");

        PASTE_COMMAND_IDS.add("org.eclipse.ui.edit.paste");
        PASTE_COMMAND_IDS.add("org.eclipse.xtend.ide.pasteJavaCode");
        PASTE_COMMAND_IDS.add("org.eclipse.jdt.ui.edit.text.java.raw.paste");
    }

    @Inject
    public ClipboardManager(IDispatcher dispatcher, IClock clock)
    {
        Preconditions.checkNotNull(dispatcher);
        Preconditions.checkNotNull(clock);
        this.dispatcher = dispatcher;
        this.clock = clock;
    }

    @Override
    public void initialize()
    {
        dispatcher.dispatchAsync(() -> {
            var display = Display.getDefault();
            display.addFilter(SWT.FocusIn, this);
            display.addFilter(SWT.FocusOut, this);
            var commandService = PlatformUI.getWorkbench().getService(ICommandService.class);
            commandService.addExecutionListener(this);
        });
    }

    @Override
    public Optional<ClipboardInfo> getClipboardInfo()
    {
        var expirationDate = this.expirationDate;
        if (expirationDate != null && clock.now().isAfter(expirationDate))
        {
            return Optional.empty();
        }

        // copyTime
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

        var info = new ClipboardInfo();
        info.text = eclipseText;
        info.path = file.map(i -> i.getFullPath().makeRelative().toPortableString()).orElse(null);
        return Optional.of(info);
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
            file = currentFile;
            text = getTextFromClipoard();
            expirationDate = clock.now().plus(MAX_DURATION);
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

    @Override
    public void handleEvent(Event event)
    {
        if (event.type == SWT.FocusOut)
        {
            currentFile = Optional.empty();
            return;
        }

        if (event.widget instanceof StyledText)
        {
            if (event.type == SWT.FocusIn)
            {
                currentFile = Optional.empty();
                for (var workbench : PlatformUI.getWorkbench().getWorkbenchWindows())
                {
                    for (var page : workbench.getPages())
                    {
                        for (var editorRef : page.getEditorReferences())
                        {
                            var editor = editorRef.getEditor(false);
                            if (editor == null)
                            {
                                continue;
                            }

                            var input = editor.getEditorInput();
                            if (input == null)
                            {
                                continue;
                            }

                            var curSourceViewer = editor.getAdapter(ITextOperationTarget.class);
                            if (curSourceViewer instanceof SourceViewer)
                            {
                                var surceViewer = (SourceViewer)curSourceViewer;
                                if (surceViewer.getTextWidget() != event.widget)
                                {
                                    continue;
                                }
                            }

                            var editorFile = input.getAdapter(IFile.class);
                            if (editorFile == null)
                            {
                                continue;
                            }

                            currentFile = Optional.of(editorFile);
                            return;
                        }
                    }
                }
            }
        }
    }
}
