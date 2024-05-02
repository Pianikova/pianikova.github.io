/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent;

import org.e1c.edt.ai.ISettingsProvider;
import org.e1c.edt.ai.ISettingsStore;

public class ResponseStreamContext implements IResponseStreamContext
{
    private final int linesCount;
    private final ILinesCounter linesCounter;

    public ResponseStreamContext(ISettingsProvider settingsProvider, ILinesCounter linesCounter)
    {
        linesCount = settingsProvider.getSettings()
            .map(settings -> settings.getCodeCompletionLinesCount())
            .orElse(ISettingsStore.DEFAULT_CODE_COMPLETION_LINES_COUNT);

        this.linesCounter = linesCounter;
    }

    @Override
    public int acceptAndGetLength(String text)
    {
        var accepted = 0;
        for (var ch : text.toCharArray())
        {
            if (linesCounter.acceptAndGetLinesCount(ch) > linesCount)
            {
                break;
            }

            accepted++;
        }

        return accepted;
    }
}
