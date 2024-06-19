/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.Optional;

import org.e1c.edt.ai.AIContext;
import org.e1c.edt.ai.ICancellationToken;
import org.e1c.edt.ai.IUISettings;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.xtext.ui.editor.XtextSourceViewer;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class AISourceViewerContextProvider
    implements IAIContextProvider<SourceViewer>
{
    private final IUISettings uiSettings;
    private final IAIContextProvider<AISourceContext> ctxProvider;

    @Inject
    public AISourceViewerContextProvider(IUISettings uiSettings, IAIContextProvider<AISourceContext> ctxProvider)
    {
        Preconditions.checkNotNull(uiSettings);
        Preconditions.checkNotNull(ctxProvider);
        this.uiSettings = uiSettings;
        this.ctxProvider = ctxProvider;
    }

    @Override
    public Optional<AIContext> create(SourceViewer sourceViewer, ICancellationToken cancellationToken)
    {
        Preconditions.checkNotNull(sourceViewer);
        var textWidget = sourceViewer.getTextWidget();
        var offset = textWidget.getCaretOffset();
        if (!(sourceViewer instanceof XtextSourceViewer))
        {
            return Optional.empty();
        }

        var xtextSourceViewer = (XtextSourceViewer)sourceViewer;
        var parseResult = xtextSourceViewer.getXtextDocument().readOnly(state -> state.getParseResult());
        if (parseResult == null)
        {
            return Optional.empty();
        }

        var sourceCtx = new AISourceContext(parseResult, offset, uiSettings.getMaxAssistantTextSize());
        sourceCtx.SkipMinorMethodStatements = true;
        return ctxProvider.create(sourceCtx, cancellationToken).or(() -> {
            sourceCtx.SkipMethodTail = true;
            return ctxProvider.create(sourceCtx, cancellationToken);
        }).or(() -> {
            sourceCtx.SkipOutOfStackStatements = true;
            return ctxProvider.create(sourceCtx, cancellationToken);
        }).or(() -> {
            sourceCtx.SkipMinorMethods = true;
            sourceCtx.Forcable = true;
            return ctxProvider.create(sourceCtx, cancellationToken);
        });
    }
}