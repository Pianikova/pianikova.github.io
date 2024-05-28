/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.Stack;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

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

import com.google.inject.Inject;

public class CodeCompletionViewModel
    implements ICodeCompletionViewModel, VerifyKeyListener, CaretListener
{
    private final Object lockObject = new Object();
    private final ILog log;
    private final IAICodeAssistant codeAssistant;
    private final IAIContextProvider aiContextProvider;
    private final IDispatcher dispatcher;
    private final IUI ui;
    private final ICodeCompletionTokenizer tokenizer;
    private final IHintPainter hintPainter;
    private final IUISettings uiSettings;
    private final Stack<String> tokens = new Stack<>();
    private CancellationToken askCancellationToken = CancellationToken.NONE;
    private StringBuilder hint = new StringBuilder();
    private boolean inProgress;
    private CompletableFuture<Void> currentResponse = CompletableFuture.completedFuture(null);

    @Inject
    public CodeCompletionViewModel(ILog log, ISettingsStore settingsStore, IAICodeAssistant codeAssistant,
        IAIContextProvider aiContextProvider,
        IDispatcher dispatcher, IUI ui,
        ICodeCompletionTokenizer tokenizer,
        IHintPainter hintPainter, IUISettings uiSettings)
    {
        this.log = log;
        this.codeAssistant = codeAssistant;
        this.aiContextProvider = aiContextProvider;
        this.dispatcher = dispatcher;
        this.ui = ui;
        this.tokenizer = tokenizer;
        this.hintPainter = hintPainter;
        this.uiSettings = uiSettings;
    }

    @Override
    public AutoCloseable activate(boolean askImmediately)
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

        if (askImmediately)
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

            log.trace("AI context " + cancellationToken.hashCode(), aiContext.toString()); //$NON-NLS-1$
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

                        log.trace("AI generated text " + cancellationToken.hashCode(), hint.toString()); //$NON-NLS-1$
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

            synchronized (lockObject)
            {
                currentResponse = response.get();
            }
        }
        catch (CancellationException e)
        {
            //  ignored
        }
        catch (Exception e)
        {
            log.logError(e);
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
                tokens.push(text);
                hint.delete(0, text.length());
                var hintLines = getHintLines();
                continueAsk(hintLines);
                if (hint.length() == 0 && currentResponse.isDone())
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
            dispatcher.dispatch(() -> {
                var hintText = hintPainter.getHintText();
                apply(viewer.get(), hintText, offset);
                tokens.push(hintText);
                hint.delete(0, hintText.length());
                var hintLines = getHintLines();
                continueAsk(hintLines);
                if (hint.length() == 0 && currentResponse.isDone())
                {
                    askByJob(0);
                }
            });

            return;
        }

        if (!uiSettings.isContinuousCodeCompletion())
        {
            reset();
            return;
        }

        if (hint.length() > 0 && hint.charAt(0) == e.character)
        {
            e.doit = false;
            var chars = new char[1];
            chars[0] = e.character;
            var text = new String(chars);
            apply(viewer.get(), text, offset);
            tokens.push(text);
            hint.delete(0, 1);
            var hintLines = getHintLines();
            continueAsk(hintLines);
            if (hint.length() == 0 && currentResponse.isDone())
            {
                askByJob(0);
            }

            return;
        }

        var charType = Character.getType(e.character);
        if (e.character == '\r' || e.character == '\n'
            || charType != Character.SPACE_SEPARATOR && charType != Character.CONTROL)
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
                    if (hintText.length() > 0)
                    {
                        viewer.getDocument().replace(offset, 0, hintText);
                        viewer.getSelectionProvider().setSelection(new TextSelection(offset + hintText.length(), 0));
                    }
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
        var maxLines = uiSettings.getCodeCompletionLinesCount();
        var lines = new StringBuilder();
        var text = hint.toString();
        while (maxLines-- > 0 && !text.isEmpty())
        {
            var token = tokenizer.getNext(2, text, this::isLineDelimiter);
            var value = token.getValue();
            lines.append(value);
            text = token.getText();
        }

        if (lines.length() == 0 || lines.charAt(lines.length() - 1) == '\n')
        {
            lines.append(System.lineSeparator());
        }

        return lines.toString();
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