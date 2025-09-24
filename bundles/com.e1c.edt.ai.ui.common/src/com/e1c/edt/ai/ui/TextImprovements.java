/**
 *
 */
package com.e1c.edt.ai.ui;

import com.google.common.base.Preconditions;

public class TextImprovements
{
    private final String uuid;
    private final String text;

    public TextImprovements(String uuid, String text)
    {
        Preconditions.checkNotNull(uuid);
        Preconditions.checkNotNull(text);
        this.uuid = uuid;
        this.text = text;
    }

    public String getUuid()
    {
        return uuid;
    }

    public String getText()
    {
        return text;
    }
}
