/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.Optional;

import org.e1c.edt.ai.Closeables;
import org.e1c.edt.ai.CodeMethod;
import org.e1c.edt.ai.ICodeCompletionStatistics;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.ITextListener;
import org.eclipse.jface.text.TextEvent;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.xtext.parser.IParseResult;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class FinalCodeFeedbackViewModel
    implements IFinalCodeFeedbackViewModel, VerifyKeyListener, ITextListener
{
    private final IDispatcher dispatcher;
    private final IUI ui;
    private final IFeedbackPainter feedbackPainter;
    private final ICodeProvider codeProvider;
    private final ICodeCompletionStatistics statistics;
    private final Object lock = new Object();
    private StyledText textWidget;
    private CodeMethod lastMethod;
    private Job lastJob;

    @Inject
    public FinalCodeFeedbackViewModel(IDispatcher dispatcher, IUI ui, IFeedbackPainter feedbackPainter,
        ICodeProvider codeProvider, ICodeCompletionStatistics statistics)
    {
        Preconditions.checkNotNull(dispatcher);
        Preconditions.checkNotNull(ui);
        Preconditions.checkNotNull(feedbackPainter);
        Preconditions.checkNotNull(codeProvider);
        Preconditions.checkNotNull(statistics);
        this.dispatcher = dispatcher;
        this.ui = ui;
        this.feedbackPainter = feedbackPainter;
        this.codeProvider = codeProvider;
        this.statistics = statistics;
    }

    @Override
    public AutoCloseable activate(StyledText textWidget)
    {
        this.textWidget = textWidget;
        return ui.getSourceViewer(textWidget).map(source -> {
            source.addTextListener(this);
            return Closeables.create(() -> source.removeTextListener(this));
        }).orElse(Closeables.Empty);
    }

    @Override
    public void verifyKey(VerifyEvent event)
    {
        if (event.character == '\0' && event.keyCode == 0x40000)
        {
            dispatcher.dispatch(() -> feedbackPainter.addStar());
        }
    }

    @Override
    public void textChanged(TextEvent event)
    {
        checkMethodWithDelay(100);
    }

    private void checkMethodWithDelay(long delay)
    {
        Job lastJob;
        synchronized (lock)
        {
            lastJob = this.lastJob;
        }

        if (lastJob != null)
        {
            lastJob.cancel();
        }

        var job = new Job(Messages.CodeCompletionJobName)
        {
            @Override
            protected IStatus run(IProgressMonitor monitor)
            {
                checkMethod();
                return Status.OK_STATUS;
            }
        };

        synchronized (lock)
        {
            this.lastJob = job;
        }

        job.schedule(delay);
    }

    private void checkMethod()
    {
        var methodOptional = getMethod();
        if (methodOptional.isEmpty())
        {
            return;
        }

        CodeMethod lastMethod;
        synchronized (lock)
        {
            lastMethod = this.lastMethod;
        }

        var method = methodOptional.get();
        if (lastMethod == null)
        {
            lastMethod = method;
        }
        else
        {
            if (!lastMethod.equals(method))
            {
                var curMethod = lastMethod;
                statistics.addMethod(lastMethod, null, i -> getMethodBody(curMethod).orElse("")); //$NON-NLS-1$
                lastMethod = method;
            }
        }

        synchronized (lock)
        {
            this.lastMethod = lastMethod;
        }
    }

    private Optional<IParseResult> getParseResult()
    {
        var parserResultOptional = dispatcher
            .dispatch(() -> ui.getTextWidget().flatMap(textWidget -> ui.getSourceViewer(textWidget)).orElse(null))
            .flatMap(sourceViewer -> codeProvider.getParseResult(sourceViewer));
        if (parserResultOptional.isEmpty())
        {
            return Optional.empty();
        }

        var parseResult = parserResultOptional.get();
        if (parseResult.hasSyntaxErrors())
        {
            return Optional.empty();
        }

        return Optional.of(parseResult);
    }

    private Optional<CodeMethod> getMethod()
    {
        var offset = dispatcher.dispatch(() -> textWidget.getCaretOffset());
        if (offset.isEmpty())
        {
            return Optional.empty();
        }

        return getParseResult()
            .flatMap(parseResult -> codeProvider.getMethod(parseResult, offset.get()));
    }

    private Optional<String> getMethodBody(CodeMethod codeMethod)
    {
        return getParseResult().flatMap(parseResult -> codeProvider.getMethodBody(parseResult, codeMethod));
    }
}
