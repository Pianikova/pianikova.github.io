/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

interface IGlobalContextStateStore
{
    GlobalContextState load();

    void save(GlobalContextState state);
}
