/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent;

import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IClock;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.IObserver;
import com.e1c.edt.ai.assistent.model.Completion;
import com.e1c.edt.ai.assistent.model.CompletionResponse;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class ResponseLineProcessor
    implements IResponseLineProcessor
{
    private final IJson json;
    private final ITextPreprocessor textPreprocessor;
    private final IClock clock;

    @Inject
    public ResponseLineProcessor(IJson json, ITextPreprocessor textPreprocessor, IClock clock)
    {
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(textPreprocessor);
        Preconditions.checkNotNull(clock);
        this.json = json;
        this.textPreprocessor = textPreprocessor;
        this.clock = clock;
    }

    @Override
    public boolean process(IObserver<Completion> observer, String line, ICancellationToken cancellationToken)
    {
        Preconditions.checkNotNull(observer);
        try
        {
            if (line == null || line.isBlank())
            {
                return true;
            }

            var startTime = clock.now();
            var sb = new StringBuilder(line.length() + 2);
            sb.append('{');
            sb.append(line);
            sb.append('}');
            return json.deserialize(sb.toString(), CompletionResponse.class)
                .map(aiResponse -> {
                    aiResponse.data.startTime = startTime;
                    return process(observer, aiResponse.data);
                })
                .orElse(true);
        }
        catch (Exception e)
        {
            observer.onError(e);
        }

        return false;
    }

    private Boolean process(IObserver<Completion> observer, Completion data)
    {
        if (data == null)
        {
            return true;
        }

        var text = data.text;
        if (text == null || text.isEmpty())
        {
            return data.finishReason == null;
        }

        data.text = textPreprocessor.process(text);
        observer.onNext(data);
        return data.finishReason == null;
    }
}