/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent;

import com.e1c.edt.ai.ISettings;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class TextPreprocessor
    implements ITextPreprocessor
{
    private final ISettings settings;

    @Inject
    public TextPreprocessor(ISettings settings)
    {
        Preconditions.checkNotNull(settings);
        this.settings = settings;
    }

    @SuppressWarnings("nls")
    @Override
    public String process(String text)
    {
        var lineSeparator = settings.getLineSeparator();
        return text.replace("\r\n", "\n").replace("\r", "\n").replace("\n", lineSeparator);
    }
}
