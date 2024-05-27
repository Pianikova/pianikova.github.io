/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

public interface ICodeCompletionViewModel
{
    AutoCloseable activate(boolean askImmediately);
}
