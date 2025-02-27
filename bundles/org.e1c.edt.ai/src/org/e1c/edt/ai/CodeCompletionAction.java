/**
 * Copyright (C) 2025, 1C
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
    FINISH,
    ROLLBACK_PART,
    ACCEPT,
    ACCEPT_PART,
    ACCEPT_LINE,
    ACCEPT_CHAR,
    TEST
}