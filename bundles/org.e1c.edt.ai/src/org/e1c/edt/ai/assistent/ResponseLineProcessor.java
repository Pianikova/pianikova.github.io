/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai.assistent;

import org.e1c.edt.ai.IJson;
import org.e1c.edt.ai.IObserver;
import org.e1c.edt.ai.assistent.model.Completion;
import org.e1c.edt.ai.assistent.model.CompletionResponse;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class ResponseLineProcessor
    implements IResponseLineProcessor
{
    private final IJson json;
    private final ITextPreprocessor textPreprocessor;

    @Inject
    public ResponseLineProcessor(IJson json, ITextPreprocessor textPreprocessor)
    {
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(textPreprocessor);
        this.json = json;
        this.textPreprocessor = textPreprocessor;
    }

    @Override
    public boolean process(IObserver<Completion> observer, String line)
    {
        Preconditions.checkNotNull(observer);
        try
        {
            if (line == null || line.isBlank())
            {
                return true;
            }

            var sb = new StringBuilder(line.length() + 2);
            sb.append('{');
            sb.append(line);
            sb.append('}');
            return json.deserialize(sb.toString(), CompletionResponse.class)
                .map(aiResponse -> process(observer, aiResponse.data))
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