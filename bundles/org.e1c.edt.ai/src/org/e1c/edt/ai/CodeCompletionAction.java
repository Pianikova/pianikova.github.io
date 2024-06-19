/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

public enum CodeCompletionAction
{
    SKIP,
    HANDLE,
    UPDATE,
    RESET,
    ASK_NEW,
    SUGGEST,
    STOP,
    ROLLBACK_PART,
    ACCEPT_PART,
    ACCEPT,
    CHAR
}