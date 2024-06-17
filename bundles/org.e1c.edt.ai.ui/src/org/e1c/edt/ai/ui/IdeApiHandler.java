/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import org.eclipse.jface.text.TextSelection;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class IdeApiHandler
{
    private IUI ui;

    @Inject
    public IdeApiHandler(IUI ui)
    {
        Preconditions.checkNotNull(ui);
        this.ui = ui;
    }

    public void wink(String parameter)
    {
        Preconditions.checkNotNull(parameter);
        System.out.println("Winked: " + parameter); //$NON-NLS-1$
    }

    public void paste_code(String code)
    {
        Preconditions.checkNotNull(code);
        ui.getTextWidget().ifPresent(textWidget -> {
            var contet = textWidget.getContent();
            ui.getSourceViewer(textWidget).ifPresent(sourceViewer -> {
                var selection = sourceViewer.getSelection();
                if (selection instanceof TextSelection)
                {
                    var textSelection = (TextSelection)selection;
                    contet.replaceTextRange(textSelection.getOffset(), textSelection.getLength(), code);
                    return;
                }

                contet.replaceTextRange(textWidget.getCaretOffset(), 0, code);
            });
        });
    }
}
