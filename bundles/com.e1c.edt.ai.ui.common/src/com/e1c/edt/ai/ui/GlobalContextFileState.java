/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.util.List;

import com.e1c.edt.ai.assistent.model.GlobalContextUpdate;

class GlobalContextFileState
{
    public long time;

    public String hash;

    public List<GlobalContextUpdate> updates;
}
