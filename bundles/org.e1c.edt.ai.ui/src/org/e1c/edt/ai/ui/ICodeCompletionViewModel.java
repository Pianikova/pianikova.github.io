/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import org.e1c.edt.ai.assistent.CancellationToken;

public interface ICodeCompletionViewModel
{
    CancellationToken activate(boolean ask);

    void deactivate();
}
