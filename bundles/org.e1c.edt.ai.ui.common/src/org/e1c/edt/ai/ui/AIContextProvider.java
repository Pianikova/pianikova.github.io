/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.Optional;

import org.e1c.edt.ai.AIContext;
import org.e1c.edt.ai.ICancellationToken;
import org.e1c.edt.ai.IContextInitializer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.xtext.ui.editor.model.IXtextDocument;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

class AIContextProvider
    implements IAIContextProvider
{
    private final IUI ui;
    private final IContentProvider contentProvider;
    private final IContextInitializer contextInitializer;

    @Inject
    public AIContextProvider(IUI ui, IContentProvider contentProvider, IContextInitializer contextInitializer)
    {
        Preconditions.checkNotNull(ui);
        Preconditions.checkNotNull(contextInitializer);
        this.ui = ui;
        this.contentProvider = contentProvider;
        this.contextInitializer = contextInitializer;
    }

    @Override
    public Optional<AIContext> create(AITarget target, ICancellationToken cancellationToken)
    {
        Preconditions.checkNotNull(target);
        Preconditions.checkNotNull(cancellationToken);
        var textWidget = target.getTextWidget();
        return ui.getSourceViewer(textWidget)
            .flatMap(sourceViewer -> create(textWidget, sourceViewer, target, cancellationToken));
    }

    private Optional<AIContext> create(StyledText textWidget, SourceViewer sourceViewer, AITarget target,
        ICancellationToken cancellationToken)
    {
        Preconditions.checkNotNull(sourceViewer);
        Preconditions.checkNotNull(target);
        Preconditions.checkNotNull(cancellationToken);
        var doc = sourceViewer.getDocument();
        var path = ""; //$NON-NLS-1$
        if (doc instanceof IXtextDocument)
        {
            var xtextDoc = (IXtextDocument)doc;
            path = xtextDoc.getResourceURI().path();
        }

        var content = contentProvider.get(textWidget);
        AIContext aiContext;
        if (target.isPreferSelection() && !content.selectionText.isBlank())
        {
            aiContext = new AIContext(textWidget.getCaretOffset(), content.text, content.offset, path,
                content.selectionText, content.selectionOffset);
        }
        else
        {
            aiContext = new AIContext(textWidget.getCaretOffset(), content.text, content.offset, path, content.text,
                content.offset);
        }

        return contextInitializer.initialize(aiContext);
    }
}