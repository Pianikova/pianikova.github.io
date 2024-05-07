/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent;

import org.e1c.edt.ai.IJson;
import org.e1c.edt.ai.IObserver;
import org.e1c.edt.ai.assistent.model.AIResponse;

public class ResponseLineProcessor implements IResponseLineProcessor
{
    public static final String DATA_LINE_PREFIX = "data:"; //$NON-NLS-1$
    private final IJson json;

    public ResponseLineProcessor(IJson json)
    {
        this.json = json;
    }

    @Override
    public boolean process(IObserver<String> observer, String line)
    {
        if (line == null || line.length() < DATA_LINE_PREFIX.length() + 1 || !line.startsWith(DATA_LINE_PREFIX))
        {
            return true;
        }

        line = line.substring(DATA_LINE_PREFIX.length());
        try
        {
            return json.deserialize(line, AIResponse.class)
                .map(aiResponse -> process(observer, aiResponse))
                .orElse(true);
        }
        catch (Exception e)
        {
            observer.onError(e);
        }

        return false;
    }

    private Boolean process(IObserver<String> observer, AIResponse aiResponse)
    {
        var token = aiResponse.getToken();
        if (token == null)
        {
            return true;
        }

        var text = token.getText();
        if (text == null || text.isEmpty())
        {
            return true;
        }

        if (aiResponse.getGeneratedText() != null)
        {
            if (text.endsWith("</s>")) //$NON-NLS-1$
            {
                return false;
            }

            observer.onNext(text);
            return false;
        }

        return observer.onNext(text);
    }
}