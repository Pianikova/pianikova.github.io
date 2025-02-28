/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import org.junit.Assert;
import org.junit.Test;

public class CancellationTokenTest
{
    @Test
    public void shouldBeNotCanceledWhenNONE()
    {
        // Given

        // When
        var tokenSource = CancellationTokens.NONE;

        // Then
        Assert.assertFalse(tokenSource.isCanceled());
    }
}
