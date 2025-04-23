/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import org.eclipse.swt.graphics.GC;

import com.google.common.base.Preconditions;

class GCTools implements IGCTools
{
    @Override
    public void copyArea(GC gc, int srcX, int srcY, int width, int height, int destX, int destY)
    {
        Preconditions.checkNotNull(gc);
        Preconditions.checkArgument(!gc.isDisposed());
        Preconditions.checkArgument(width > 0);
        Preconditions.checkArgument(height > 0);
        gc.copyArea(srcX, srcY, width, height, destX, destY, true);
        /*var buffer = new Image(gc.getDevice(), width, height);
        try
        {
            gc.copyArea(buffer, srcX, srcY);
            gc.drawImage(buffer, destX, destY);
        }
        finally
        {
            buffer.dispose();
        }*/
    }
}
