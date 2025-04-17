/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;

import com.google.common.base.Preconditions;

class GCTools implements IGCTools
{
    @Override
    @SuppressWarnings("nls")
    public void copyArea(GC gc, int srcX, int srcY, int width, int height, int destX, int destY)
    {
        Preconditions.checkNotNull(gc);
        Preconditions.checkArgument(!gc.isDisposed(), "gc is disposed");
        Preconditions.checkArgument(width > 0, "width must be positive");
        Preconditions.checkArgument(height > 0, "height must be positive");
        var buffer = new Image(gc.getDevice(), width, height);
        try
        {
            gc.copyArea(buffer, srcX, srcY);
            gc.drawImage(buffer, destX, destY);
        }
        finally
        {
            buffer.dispose();
        }
    }
}
