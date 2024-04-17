/**
 * Copyright (C) 2024, 1C
*/
package org.e1c.edt.ai.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.e1c.edt.ai.assistent.CancellationToken;
import org.e1c.edt.ai.assistent.IAICodeAssistant;
import org.e1c.edt.ai.assistent.model.AITextResponse;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.codemining.AbstractCodeMiningProvider;
import org.eclipse.jface.text.codemining.CodeMiningReconciler;
import org.eclipse.jface.text.codemining.ICodeMining;
import org.eclipse.jface.text.codemining.ICodeMiningProvider;
import org.eclipse.jface.text.codemining.LineContentCodeMining;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Display;

public class CodeMiningProvider
    extends AbstractCodeMiningProvider
{
    private final ExecutorService threadPool = Executors.newSingleThreadExecutor();
    private final CodeMiningReconciler reconciler = new CodeMiningReconciler();
    private AiCodeCompletion completion;

    public CodeMiningProvider()
    {
        reconciler.setDelay(500);
    }

    @Override
    public CompletableFuture<List<? extends ICodeMining>> provideCodeMinings(ITextViewer viewer,
        IProgressMonitor monitor)
    {
        if (completion == null)
        {
            Display.getDefault().syncExec(() -> reconciler.install(viewer));
        }

        return CompletableFuture.supplyAsync(() -> {
            List<ICodeMining> minings = new ArrayList<>();
            if (Activator.getDefault().getPreferenceStore().getBoolean(Activator.PREF_CODE_COMPLITION_ENABLED))
            {
                if (completion != null)
                {
                    completion.dispose();
                }

                completion = new AiCodeCompletion(Composition.getCodeAssistant(), threadPool, this, viewer,
                    getAiCodeCompletionInfo(viewer));
                minings.add(completion);
            }

            return minings;
        }, threadPool);
    }

    @Override
    public void dispose()
    {
        reconciler.uninstall();
        threadPool.shutdown();
        if (completion != null)
        {
            completion.dispose();
        }

        super.dispose();
    }

    private static AiCodeCompletionInfo getAiCodeCompletionInfo(ITextViewer viewer)
    {
        IDocument document = viewer.getDocument();
        int linesCount = document.getNumberOfLines();

        final ArrayList<Integer> lines = new ArrayList<>();
        Display.getDefault().syncExec(() -> {
            ITextSelection selection = (ITextSelection)viewer.getSelectionProvider().getSelection();
            lines.add(selection.getEndLine());
        });

        int curLine = lines.get(0);
        if (curLine < linesCount)
        {
            try
            {
                IRegion curLineInfo = document.getLineInformation(curLine);
                while (curLine-- >= 0)
                {
                    IRegion lineInfo = document.getLineInformation(curLine);
                    if (lineInfo.getLength() > 0)
                    {
                        int offset = lineInfo.getOffset() + lineInfo.getLength();
                        String delimiter = document.getLineDelimiter(curLine);
                        int delimiterLength = delimiter.length();
                        return new AiCodeCompletionInfo(new Position(offset, delimiterLength),
                            curLineInfo.getOffset() + curLineInfo.getLength());
                    }
                }
            }
            catch (BadLocationException e)
            {
                Activator.logError(e);
            }
        }

        // show at the end
        Position position = new Position(document.getLength(), 0);
        position.delete();
        return new AiCodeCompletionInfo(position, document.getLength());
    }

    public static class AiCodeCompletionInfo
    {
        private Position position;
        private int offset;

        public AiCodeCompletionInfo(Position position, int offset)
        {
            this.position = position;
            this.offset = offset;
        }

        public Position getPosition()
        {
            return position;
        }

        public int getOffset()
        {
            return offset;
        }
    }

    public static class AiCodeCompletion
        extends LineContentCodeMining
    {
        private static final String AI_PREFIX = Messages.AI_Prefix;
        private static final String AI_SUGGESTIONS = AI_PREFIX + Messages.AI_Suggestions;
        private static final String AI_THINKING = AI_PREFIX + Messages.AI_Thinking;

        private final Object lockObject = new Object();
        private final ITextViewer viewer;
        private final IAICodeAssistant codeAssistant;
        private final int offset;
        private CancellationToken askCancellationToken = new CancellationToken();
        private ExecutorService threadPool;

        protected AiCodeCompletion(IAICodeAssistant codeAssistant, ExecutorService threadPool,
            ICodeMiningProvider provider,
            ITextViewer viewer,
            AiCodeCompletionInfo info)
        {
            super(info.getPosition(), provider);
            this.codeAssistant = codeAssistant;
            this.threadPool = threadPool;
            this.viewer = viewer;
            offset = info.getOffset();
            askAsync();
        }

        @Override
        public Consumer<MouseEvent> getAction()
        {
            return e -> apply();
        }

        @Override
        protected CompletableFuture<Void> doResolve(ITextViewer viewer, IProgressMonitor monitor)
        {
            return CompletableFuture.runAsync(() -> askAsync());
        }

        private void askAsync()
        {
            CancellationToken cancellationToken = new CancellationToken();
            synchronized (lockObject)
            {
                askCancellationToken.cancel();
                askCancellationToken = cancellationToken;
            }

            threadPool.execute(() -> ask(cancellationToken));
        }

        private void ask(CancellationToken cancellationToken)
        {
            try
            {
                if (cancellationToken.isCanceled())
                {
                    return;
                }

                setLabel(AI_THINKING);

                IDocument document = viewer.getDocument();
                String text = document.get(0, offset);
                AITextResponse responce = codeAssistant.generateText(text, cancellationToken);
                text = responce.getGeneratedText();

                if (cancellationToken.isCanceled())
                {
                    return;
                }

                setLabel(text.isBlank() ? AI_SUGGESTIONS : text);
                redraw();
            }
            catch (CancellationException e)
            {
                //  ignored
            }
            catch (Exception e)
            {
                Activator.logError(e);
            }
        }

        private void apply()
        {
            try
            {
                String code = super.getLabel();
                if (code.startsWith(AI_PREFIX))
                {
                    return;
                }

                setLabel(AI_SUGGESTIONS);

                IDocument document = viewer.getDocument();
                document.replace(offset, 0, code + System.lineSeparator());
                viewer.getSelectionProvider().setSelection(new TextSelection(offset + code.length(), 0));
            }
            catch (BadLocationException e)
            {
                Activator.logError(e);
            }
        }

        private void redraw()
        {
            Display.getDefault().syncExec(() -> viewer.getTextWidget().redraw());
        }

        @Override
        public void dispose()
        {
            synchronized (lockObject)
            {
                askCancellationToken.cancel();
            }

            super.dispose();
        }
    }
}
