/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import org.e1c.edt.ai.assistent.ITextPreprocessor;
import org.eclipse.jface.text.TextSelection;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class IdeApiHandler
{
    private final IUI ui;
    private final ITextPreprocessor textPreprocessor;

    @Inject
    public IdeApiHandler(IUI ui, ITextPreprocessor textPreprocessor)
    {
        Preconditions.checkNotNull(ui);
        Preconditions.checkNotNull(textPreprocessor);
        this.ui = ui;
        this.textPreprocessor = textPreprocessor;
    }

    public void wink(String parameter)
    {
        Preconditions.checkNotNull(parameter);
        System.out.println("Winked: " + parameter); //$NON-NLS-1$
    }

    public void paste_code(String code)
    {
        if (code == null)
        {
            return;
        }

        final var processedCode = textPreprocessor.process(code);
        ui.getTextWidget().ifPresent(textWidget -> {
            var contet = textWidget.getContent();
            ui.getSourceViewer(textWidget).ifPresent(sourceViewer -> {
                var selection = sourceViewer.getSelection();
                if (selection instanceof TextSelection)
                {
                    var textSelection = (TextSelection)selection;
                    contet.replaceTextRange(textSelection.getOffset(), textSelection.getLength(), processedCode);
                    return;
                }

                contet.replaceTextRange(textWidget.getCaretOffset(), 0, processedCode);
            });
        });
    }
}
