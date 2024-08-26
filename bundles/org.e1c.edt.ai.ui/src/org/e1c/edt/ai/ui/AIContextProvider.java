/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.Optional;

import org.e1c.edt.ai.AIContext;
import org.e1c.edt.ai.ICancellationToken;
import org.e1c.edt.ai.IContextInitializer;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.xtext.ui.editor.model.IXtextDocument;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class AIContextProvider
    implements IAIContextProvider<Void>
{
    private final IUI ui;
    private final IContextInitializer contextInitializer;

    @Inject
    public AIContextProvider(IUI ui, IContextInitializer contextInitializer)
    {
        Preconditions.checkNotNull(ui);
        Preconditions.checkNotNull(contextInitializer);
        this.ui = ui;
        this.contextInitializer = contextInitializer;
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
        var source = textWidget.getText();
        var sourceOffset = textWidget.getCaretOffset();
        var text = source;
        var textOffset = sourceOffset;
        if (target.isPreferSelection())
        {
            var selection = sourceViewer.getSelection();
            if (!selection.isEmpty())
            {
                if (selection instanceof ITextSelection)
                {
                    var textSelection = (ITextSelection)selection;
                    var selectionOffset = sourceOffset - textSelection.getOffset();
                    if (selectionOffset >= 0 && selectionOffset <= textSelection.getLength())
                    {
                        textOffset = selectionOffset;
                        text = textSelection.getText();
                    }
                }
            }
        }

        var doc = sourceViewer.getDocument();
        var path = ""; //$NON-NLS-1$
        if (doc instanceof IXtextDocument)
        {
            var xtextDoc = (IXtextDocument)doc;
            path = xtextDoc.getResourceURI().path();
        }

        return contextInitializer
            .initialize(new AIContext(source, sourceOffset, path, text, textOffset));
    }
}