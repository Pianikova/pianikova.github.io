/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.Optional;

import org.e1c.edt.ai.AIContext;
import org.e1c.edt.ai.CancellationToken;
import org.e1c.edt.ai.IAIContextFactory;
import org.eclipse.jface.text.source.SourceViewer;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class AIContextProvider
    implements IAIContextProvider<Void>
{
    private final IUI ui;
    private final IUISettings uiSettings;
    private final IAIContextFactory contextFactory;
    private final IAIContextProvider<SourceViewer> sourceBasedContextProvider;

    @Inject
    public AIContextProvider(IUI ui,
        IUISettings uiSettings,
        IAIContextFactory contextFactory,
        IAIContextProvider<SourceViewer> sourceBasedContextProvider)
    {
        Preconditions.checkNotNull(ui);
        Preconditions.checkNotNull(uiSettings);
        Preconditions.checkNotNull(contextFactory);
        Preconditions.checkNotNull(sourceBasedContextProvider);
        this.ui = ui;
        this.uiSettings = uiSettings;
        this.contextFactory = contextFactory;
        this.sourceBasedContextProvider = sourceBasedContextProvider;
    }

    @Override
    public Optional<AIContext> create(Void state, CancellationToken cancellationToken)
    {
        return ui.getTextWidget()
            .flatMap(textWidget -> ui.getSourceViewer(textWidget))
            .flatMap(sourceViewer -> {
                var textWidget = sourceViewer.getTextWidget();
                var text = textWidget.getText();
                var offsset = textWidget.getCaretOffset();
                if (text.length() <= uiSettings.getMaxAssistantTextSize())
                {
                    return contextFactory.create(text, offsset);
                }

                return sourceBasedContextProvider.create(sourceViewer, cancellationToken)
                    .or(() -> contextFactory.create(text, offsset));
            });
    }
}