/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.Optional;

import org.e1c.edt.ai.AIContext;
import org.e1c.edt.ai.ICancellationToken;
import org.e1c.edt.ai.IContextFactory;
import org.e1c.edt.ai.IUISettings;
import org.eclipse.jface.text.source.SourceViewer;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class AIContextProvider
    implements IAIContextProvider<Void>
{
    private final IUI ui;
    private final IUISettings uiSettings;
    private final IContextFactory contextFactory;

    @Inject
    public AIContextProvider(IUI ui,
        IUISettings uiSettings,
        IContextFactory contextFactory)
    {
        Preconditions.checkNotNull(ui);
        Preconditions.checkNotNull(uiSettings);
        Preconditions.checkNotNull(contextFactory);
        this.ui = ui;
        this.uiSettings = uiSettings;
        this.contextFactory = contextFactory;
    }

    @Override
    public Optional<AIContext> create(AITarget target, Void state, ICancellationToken cancellationToken)
    {
        Preconditions.checkNotNull(target);
        Preconditions.checkNotNull(cancellationToken);
        return ui.getSourceViewer(target.getTextWidget())
            .flatMap(sourceViewer -> create(sourceViewer, target, state, cancellationToken));
    }

    private Optional<AIContext> create(SourceViewer sourceViewer, AITarget target, Void state,
        ICancellationToken cancellationToken)
    {
        Preconditions.checkNotNull(sourceViewer);
        Preconditions.checkNotNull(target);
        Preconditions.checkNotNull(cancellationToken);
        var textWidget = sourceViewer.getTextWidget();
        var text = textWidget.getText();
        var offset = textWidget.getCaretOffset();
        var max = target.getMaxLength();
        if (max <= 0)
        {
            max = uiSettings.getMaxAssistantTextSize();
        }

        return contextFactory.create(text, offset, text, offset);
    }
}