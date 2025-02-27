/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai.assistent;

import org.e1c.edt.ai.IUISettings;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class TextPreprocessor
    implements ITextPreprocessor
{
    private final IUISettings uiSettings;

    @Inject
    public TextPreprocessor(IUISettings uiSettings)
    {
        Preconditions.checkNotNull(uiSettings);
        this.uiSettings = uiSettings;
    }

    @SuppressWarnings("nls")
    @Override
    public String process(String text)
    {
        var lineSeparator = uiSettings.getLineSeparator();
        return text.replace("\r\n", "\n").replace("\r", "\n").replace("\n", lineSeparator);
    }
}
