/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.custom.StyledText;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

class ContentProvider
    implements IContentProvider
{
    private final IUI ui;

    @Inject
    public ContentProvider(IUI ui)
    {
        Preconditions.checkNotNull(ui);
        this.ui = ui;
    }

    @Override
    public Content get(StyledText textWidget, int offset)
    {
        return ui.getSourceViewer(textWidget)
            .map(sourceViewer -> get(textWidget, sourceViewer, offset))
            .orElseGet(() -> new Content(textWidget.getText(), offset, "", 0)); //$NON-NLS-1$
    }

    private Content get(StyledText textWidget, SourceViewer sourceViewer, int offset)
    {
        var text = sourceViewer.getDocument().get();
        var widgetOffset = sourceViewer.widgetOffset2ModelOffset(offset);
        var selection = sourceViewer.getSelection();
        if (selection.isEmpty() || !(selection instanceof ITextSelection))
        {
            return new Content(text, widgetOffset, "", 0); //$NON-NLS-1$
        }

        var textSelection = (ITextSelection)selection;
        var selectionStart = textSelection.getOffset();
        var selectionFinish = textSelection.getOffset() + textSelection.getLength();
        var selectionText = text.substring(selectionStart, selectionFinish);
        return new Content(text, widgetOffset, selectionText, widgetOffset - selectionStart);
    }
}
