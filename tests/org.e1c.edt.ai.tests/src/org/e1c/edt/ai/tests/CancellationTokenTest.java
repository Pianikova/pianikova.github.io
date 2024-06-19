/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.tests;

import org.e1c.edt.ai.CancellationTokens;
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
