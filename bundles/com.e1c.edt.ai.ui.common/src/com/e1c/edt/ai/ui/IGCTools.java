/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import org.eclipse.swt.graphics.GC;

interface IGCTools
{
    void copyArea(GC gc, int srcX, int srcY, int width, int height, int destX, int destY);
}
