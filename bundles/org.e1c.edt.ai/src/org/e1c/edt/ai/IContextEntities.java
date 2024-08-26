/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import org.e1c.edt.ai.assistent.model.LocalContext;

public interface IContextEntities
{
    void fill(AIContext aiContext, LocalContext context);
}
