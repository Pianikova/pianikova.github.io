/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.util.Optional;

import com.e1c.edt.ai.assistent.model.ClipboardInfo;

public interface IClipboard
{
    Optional<ClipboardInfo> getClipboardInfo();

    boolean isPasting();
}
