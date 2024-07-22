/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import java.util.Optional;

import org.e1c.edt.ai.assistent.model.CursorInfo;

public interface ICursorInfoProvider
{
    Optional<CursorInfo> getCursorInfo(int cursorOffset);
}
