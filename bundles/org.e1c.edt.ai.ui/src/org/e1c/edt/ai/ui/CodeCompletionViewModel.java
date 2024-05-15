/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.Stack;
import java.util.concurrent.CancellationException;

import org.e1c.edt.ai.CancellationToken;
import org.e1c.edt.ai.Closeables;
import org.e1c.edt.ai.ICodeCompletionTokenizer;
import org.e1c.edt.ai.ILog;
import org.e1c.edt.ai.IObserver;
import org.e1c.edt.ai.ISettingsStore;
import org.e1c.edt.ai.assistent.IAICodeAssistant;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CaretEvent;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.VerifyEvent;

public class CodeCompletionViewModel
    implements ICodeCompletionViewModel, VerifyKeyListener, CaretListener
{
    private final Object lockObject = new Object();
    private final ILog log;
    private final ISettingsStore settingsStore;
    private final IAICodeAssistant codeAssistant;
    private final IAIContextProvider aiContextProvider;
    private final IDispatcher dispatcher;
    private final IUI ui;
    private final ICodeCompletionTokenizer tokenizer;
    private final IHintPainter hintPainter;
    private final Stack<String> tokens = new Stack<>();
    private CancellationToken askCancellationToken = CancellationToken.NONE;
    private StringBuilder hint = new StringBuilder();
    private boolean inProgress;

    public CodeCompletionViewModel(ILog log, ISettingsStore settingsStore, IAICodeAssistant codeAssistant,
        IAIContextProvider aiContextProvider,
        IDispatcher dispatcher, IUI ui,
        ICodeCompletionTokenizer tokenizer,
        IHintPainter hintPainter)
    {
        this.log = log;
        this.settingsStore = settingsStore;
        this.codeAssistant = codeAssistant;
        this.aiContextProvider = aiContextProvider;
        this.dispatcher = dispatcher;
        this.ui = ui;
        this.tokenizer = tokenizer;
        this.hintPainter = hintPainter;
    }

    @Override
    public AutoCloseable activate(boolean ask)
    {
        cancel();
        dispatcher.dispatch(() -> {
            hintPainter.reset();
            ui.getTextViewerExtension2().ifPresent(viewer -> viewer.addPainter(hintPainter));
            ui.getTextViewer().ifPresent(viewer -> {
                var textWidget = viewer.getTextWidget();
                textWidget.addCaretListener(this);
                textWidget.addVerifyKeyListener(this);
            });
        });

        if (ask)
        {
            askByJob(0);
        }

        return Closeables.create(() -> deactivate());
    }

    private void deactivate()
    {
        cancel();
        dispatcher.dispatch(() -> {
            ui.getTextViewerExtension2().ifPresent(viewer -> viewer.removePainter(hintPainter));
            ui.getTextViewer().ifPresent(viewer -> {
                var textWidget = viewer.getTextWidget();
                textWidget.removeCaretListener(this);
                textWidget.removeVerifyKeyListener(this);
            });
        });

        reset();
    }

    private void reset()
    {
        synchronized (lockObject)
        {
            if (!cancel())
            {
                return;
            }
        }

        dispatcher.dispatch(() -> {
            hint = new StringBuilder();
            tokens.clear();
            inProgress = false;
            hintPainter.reset();
        });
    }

    private boolean cancel()
    {
        synchronized (lockObject)
        {
            if (askCancellationToken.isCanceled())
            {
                return false;
            }

            askCancellationToken.cancel();
            return true;
        }
    }

    private void askByJob(long delay)
    {
        cancel();
        new Job(Messages.CodeCompletionJobName)
        {
            @Override
            protected IStatus run(IProgressMonitor monitor)
            {
                var cancellationToken = new JobCancellationToken();
                synchronized (lockObject)
                {
                    askCancellationToken = cancellationToken;
                }

                cancellationToken.attachMonitor(monitor);
                ask(cancellationToken);
                return cancellationToken.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
            }
        }.schedule(delay);
    }

    private void ask(CancellationToken cancellationToken)
    {
        try
        {
            var ctx = dispatcher.dispatch(() -> aiContextProvider.create().orElse(null));
            if (cancellationToken.isCanceled() || ctx.isEmpty())
            {
                return;
            }

            var aiContext = ctx.get();
            var context = aiContext.getContext();
            if (context == null || context.isBlank())
            {
                return;
            }

            var observer = new IObserver<String>()
            {
                @Override
                public boolean onNext(String value)
                {
                    dispatcher.dispatch(() -> {
                        if (cancellationToken.isCanceled())
                        {
                            return;
                        }

                        hint.append(value);
                        hintPainter.setHintAt(aiContext.getCursorOffset(), getHintLines());
                    });

                    return true;
                }

                @Override
                public void onError(Throwable error)
                {
                    if (cancellationToken.isCanceled())
                    {
                        return;
                    }

                    log.logError(error);
                    reset();
                }

                @Override
                public void onCompleted()
                {
                    dispatcher.dispatch(() -> {
                        if (cancellationToken.isCanceled())
                        {
                            return;
                        }

                        hintPainter.setHintAt(aiContext.getCursorOffset(), getHintLines());
                    });
                }
            };

            dispatcher.dispatch(() -> {
                if (cancellationToken.isCanceled())
                {
                    return;
                }

                hintPainter.reset();
                hintPainter.pinOffset(aiContext.getCursorOffset());
            });

            var response = codeAssistant.generateText(context, observer, cancellationToken);
            if (cancellationToken.isCanceled() || response.isEmpty())
            {
                return;
            }
        }
        catch (CancellationException e)
        {
            //  ignored
        }
        catch (Exception e)
        {
            Activator.logError(e);
            deactivate();
        }
    }

    @Override
    public void verifyKey(VerifyEvent e)
    {
        var offset = hintPainter.getOffset();
        var viewer = ui.getTextViewer();
        if (viewer.isEmpty())
        {
            return;
        }

        if (e.keyCode == SWT.ESC && offset >= 0)
        {
            reset();
        }

        if (e.keyCode == SWT.ARROW_RIGHT && offset >= 0)
        {
            e.doit = false;
            dispatcher.dispatch(() -> {
                var hintText = hintPainter.getHintText();
                var token = tokenizer.getNext(1, hintText, this::isTextDelimiter);
                var text = token.getValue();
                apply(viewer.get(), text, offset);
                hint.delete(0, text.length());
                tokens.push(text);
                var hintLines = getHintLines();
                continueAsk(hintLines);
                if (hint.length() == 0)
                {
                    askByJob(0);
                }
            });

            return;
        }

        if (e.keyCode == SWT.ARROW_LEFT && offset >= 0)
        {
            e.doit = false;
            dispatcher.dispatch(() -> {
                if (tokens.size() > 0)
                {
                    var text = tokens.pop();
                    rollback(viewer.get(), text, offset);
                    hint.insert(0, text);
                    var hintLines = getHintLines();
                    continueAsk(hintLines);
                }
            });

            return;
        }

        if (e.character == '\t' && offset >= 0)
        {
            e.doit = false;
            var text = hintPainter.getHintText();
            dispatcher.dispatch(() -> {
                apply(viewer.get(), text, offset);
                hint.delete(0, text.length());
                tokens.push(text);
                var hintLines = getHintLines();
                continueAsk(hintLines);
                if (hint.length() == 0)
                {
                    askByJob(0);
                }
            });

            return;
        }

        if (!isContinuousCodeCompletion())
        {
            reset();
            return;
        }

        var charType = Character.getType(e.character);
        if (charType != Character.SPACE_SEPARATOR && charType != Character.CONTROL)
        {
            askByJob(500);
        }
    }

    private void apply(ITextViewer viewer, String hintText, int offset)
    {
        try
        {
            if (!hintText.isEmpty())
            {
                try
                {
                    inProgress = true;
                    viewer.getDocument().replace(offset, 0, hintText);
                    viewer.getSelectionProvider().setSelection(new TextSelection(offset + hintText.length(), 0));
                }
                finally
                {
                    inProgress = false;
                }
            }
        }
        catch (BadLocationException e)
        {
            log.logError(e);
        }
    }

    private void rollback(ITextViewer viewer, String hintText, int offset)
    {
        try
        {
            if (!hintText.isEmpty())
            {
                try
                {
                    inProgress = true;
                    var start = offset - hintText.length();
                    viewer.getDocument().replace(start, hintText.length(), ""); //$NON-NLS-1$
                    viewer.getSelectionProvider().setSelection(new TextSelection(start, 0));
                }
                finally
                {
                    inProgress = false;
                }
            }
        }
        catch (BadLocationException e)
        {
            log.logError(e);
        }
    }

    private void continueAsk(String hint)
    {
        aiContextProvider.create().ifPresent(ctx -> {
            hintPainter.pinOffset(ctx.getCursorOffset());
            hintPainter.setHintAt(ctx.getCursorOffset(), hint);
            if (hint.isEmpty())
            {
                askByJob(0);
            }
        });
    }

    @Override
    public void caretMoved(CaretEvent event)
    {
        if (!inProgress)
        {
            reset();
        }
    }

    private String getHintLines()
    {
        var maxLines = getMaxLines();
        var lines = new StringBuilder();
        var text = hint.toString();
        while (maxLines-- > 0 && !text.isEmpty())
        {
            var token = tokenizer.getNext(2, text, this::isLineDelimiter);
            lines.append(token.getValue());
            text = token.getText();
        }

        if (lines.length() == 0 || lines.charAt(lines.length() - 1) == '\n')
        {
            lines.append(System.lineSeparator());
        }

        return lines.toString();
    }

    private int getMaxLines()
    {
        return settingsStore.getInt(ISettingsStore.CODE_COMPLETION_LINES_COUNT);
    }

    private boolean isContinuousCodeCompletion()
    {
        return settingsStore.getBoolean(ISettingsStore.CONTINUOUS_CODE_COMPLETION);
    }

    private Boolean isTextDelimiter(char ch)
    {
        return (ch == ' ') || (ch == '\t') || isLineDelimiter(ch);
    }

    private Boolean isLineDelimiter(char ch)
    {
        return (ch == '\n') || (ch == '\r');
    }
}