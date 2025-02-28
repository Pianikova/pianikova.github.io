/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import java.util.Optional;

import com.e1c.edt.ai.assistent.model.CursorInfo;

public interface ICursorInfoProvider
{
    Optional<CursorInfo> getCursorInfo(int cursorOffset);
}
