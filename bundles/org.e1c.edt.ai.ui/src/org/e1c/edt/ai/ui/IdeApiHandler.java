/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

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
            contet.replaceTextRange(textWidget.getCaretOffset(), 0, code);
        });
    }
}
