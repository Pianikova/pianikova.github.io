/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.Optional;

import org.e1c.edt.ai.AIContext;
import org.e1c.edt.ai.IAIContextFactory;
import org.e1c.edt.ai.ICancellationToken;
import org.e1c.edt.ai.IUISettings;
import org.eclipse.jface.text.source.SourceViewer;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class AIContextProvider
    implements IAIContextProvider<AITarget>
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
    public Optional<AIContext> create(AITarget target, ICancellationToken cancellationToken)
    {
        return ui.getSourceViewer(target.getTextWidget())
            .flatMap(sourceViewer -> {
                var textWidget = sourceViewer.getTextWidget();
                var text = textWidget.getText();
                var offset = textWidget.getCaretOffset();
                var max = target.getMaxLength();
                if (max <= 0)
                {
                    max = uiSettings.getMaxAssistantTextSize();
                }

                if (max == Integer.MAX_VALUE)
                {
                    if (textWidget.isTextSelected())
                    {
                        text = textWidget.getSelectionText();
                        offset = 0;
                    }
                }

                if (text.length() <= max)
                {
                    return contextFactory.create(text, offset);
                }

                var curText = text;
                var curOffset = offset;
                return sourceBasedContextProvider.create(sourceViewer, cancellationToken)
                    .or(() -> contextFactory.create(curText, curOffset));
            });
    }
}