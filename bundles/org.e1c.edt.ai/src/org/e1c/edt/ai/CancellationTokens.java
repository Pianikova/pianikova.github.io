/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

public class CancellationTokens
{
    public final static ICancellationToken NONE = new ICancellationToken()
    {
        @Override
        public Boolean isCanceled()
        {
            return false;
        }
    };
}