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
    public boolean process(IResponseStreamContext context, IObserver<String> observer, String line)
    {
        if (line == null || line.length() < DATA_LINE_PREFIX.length() + 1 || !line.startsWith(DATA_LINE_PREFIX))
        {
            return true;
        }

        line = line.substring(DATA_LINE_PREFIX.length());
        try
        {
            var aiResponse = json.deserialize(line, AIResponse.class);
            var generatedText = aiResponse.getGeneratedText();
            if (generatedText != null)
            {
                var update = createUpdate(context, generatedText);
                observer.onNext(update);
                return false;
            }

            var token = aiResponse.getToken();
            if (token != null)
            {
                var text = token.getText();
                if (text != null && !text.isEmpty())
                {
                    var update = createUpdate(context, text);
                    return observer.onNext(update.toString()) && !isPartialUpdate(text, update);
                }
            }

            return true;
        }
        catch (Exception e)
        {
            observer.onError(e);
        }

        return false;
    }

    private boolean isPartialUpdate(String text, String update)
    {
        return update.length() < text.length();
    }

    private String createUpdate(IResponseStreamContext context, String text)
    {
        if (text.isEmpty())
        {
            return text;
        }

        return text.substring(0, context.acceptAndGetLength(text));
    }
}