/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import java.util.ArrayList;

import org.e1c.edt.ai.assistent.IFeedbackService;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class CodeCompletionStatistics
    implements ICodeCompletionContext
{
    private final IFeedbackService feedbackService;
    private final ArrayList<Text> code = new ArrayList<>();

    @Inject
    public CodeCompletionStatistics(IFeedbackService feedbackService)
    {
        Preconditions.checkNotNull(feedbackService);
        this.feedbackService = feedbackService;
    }

    @Override
    public void apply(Text text, int offset)
    {
        if (offset < 0)
        {
            return;
        }

        if (text.getSource().getId().isBlank())
        {
            return;
        }

        code.add(text);
    }

    @Override
    public void rollback(int offset, int length)
    {
        while (length > 0 && code.size() > 0)
        {
            var index = code.size() - 1;
            var lastText = code.get(index);
            code.remove(index);
            var text = lastText.getText();
            var len = text.length();
            if (len <= length)
            {
                length -= len;
            }
            else
            {
                length = 0;
                code.add(new Text(text.substring(0, len - length), lastText.getSource()));
            }
        }
    }

    @Override
    public void commit()
    {
        if (code.isEmpty())
        {
            return;
        }

        try
        {
            String lastSourceId = null;
            var sb = new StringBuilder();
            for (var text : code)
            {
                var sourceId = text.getSource().getId();
                if (!sourceId.equals(lastSourceId))
                {
                    lastSourceId = sourceId;
                    if (sb.length() > 0)
                    {
                        feedbackService.acceptedCodeAsync(lastSourceId, sb.toString());
                        sb.setLength(0);
                    }
                }

                sb.append(text.getText());
            }

            if (sb.length() > 0)
            {
                feedbackService.acceptedCodeAsync(lastSourceId, sb.toString());
            }
        }
        finally
        {
            code.clear();
        }
    }
}
