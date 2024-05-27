/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import org.e1c.edt.ai.IHintTextBuilder;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class HintPainterProvider
    implements Provider<IHintPainter>
{
    private final IUI ui;
    private final IHintTextBuilder hintTextBuilder;
    private final IUISettings uiSettings;

    @Inject
    public HintPainterProvider(IUI ui, IHintTextBuilder hintTextBuilder, IUISettings uiSettings)
    {
        this.ui = ui;
        this.hintTextBuilder = hintTextBuilder;
        this.uiSettings = uiSettings;
    }

    @Override
    public IHintPainter get()
    {
        var textViewer = ui.getTextViewer().orElseThrow();
        var hintPainter = new HintPainter(textViewer, hintTextBuilder, uiSettings);
        hintPainter.setLabel("Tab → ← Esc"); //$NON-NLS-1$
        return hintPainter;
    }

}
