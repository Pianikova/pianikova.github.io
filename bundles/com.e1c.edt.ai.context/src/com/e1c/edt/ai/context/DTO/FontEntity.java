/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context.DTO;

import com.e1c.edt.ai.assistent.model.IContextEntity;

public class FontEntity
    implements IContextEntity
{
    public boolean bold;

    public boolean italic;

    public boolean underline;

    public boolean strikeout;

    public String faceName;

    public int scale;

    public int height;
}
