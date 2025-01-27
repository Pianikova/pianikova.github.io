/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai.ui;

interface IGlobalContextStateStore
{
    GlobalContextState load();

    void save(GlobalContextState state);
}
