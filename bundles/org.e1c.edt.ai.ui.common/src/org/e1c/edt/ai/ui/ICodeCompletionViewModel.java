/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai.ui;

import org.e1c.edt.ai.ICodeCompletionContext;
import org.eclipse.swt.custom.StyledText;

interface ICodeCompletionViewModel<TContext extends ICodeCompletionContext>
{
    AutoCloseable activate(StyledText textWidget);
}