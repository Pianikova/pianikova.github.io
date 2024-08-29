/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent;

import org.e1c.edt.ai.IUISettings;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class TextPreprocessor implements ITextPreprocessor
{
    private final IUISettings uiSettings;

    @Inject
    public TextPreprocessor(IUISettings uiSettings)
    {
        Preconditions.checkNotNull(uiSettings);
        this.uiSettings = uiSettings;
    }

    @Override
    public String process(String text)
    {
        var lineSeparator = uiSettings.getLineSeparator();
        if (lineSeparator.length() == 1 && lineSeparator.charAt(0) == '\n')
        {
            return text;

        }

        return text.replace("\n", lineSeparator); //$NON-NLS-1$
    }
}
