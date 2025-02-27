/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Assert;
import org.junit.Test;

public class CancellationTokenSourceTest
{
    @Test
    public void shouldBeNotCanceledWhenCreted()
    {
        // Given

        // When
        var tokenSource = new CancellationTokenSource();

        // Then
        Assert.assertFalse(tokenSource.isCanceled());
    }

    @Test
    public void shouldBeCanceledWhenCanceled()
    {
        // Given
        var tokenSource = new CancellationTokenSource();

        // When
        tokenSource.cancel();

        // Then
        Assert.assertTrue(tokenSource.isCanceled());
    }

    @Test
    public void shouldRunAttcahedHandler()
    {
        // Given
        var tokenSource = new CancellationTokenSource();
        var runnable = mock(Runnable.class);

        // When
        try (var attachToken = CancellationTokenSource.attach(tokenSource, runnable))
        {
            tokenSource.cancel();
        }
        catch (Exception e)
        {
            Assert.fail(e.getMessage());
        }


        // Then
        Assert.assertTrue(tokenSource.isCanceled());
        verify(runnable).run();
    }

    @Test
    public void shouldRunAttcahedHandlerOnceWhenFewCancellation()
    {
        // Given
        var tokenSource = new CancellationTokenSource();
        var runnable = mock(Runnable.class);

        // When
        try (var attachToken = CancellationTokenSource.attach(tokenSource, runnable))
        {
            tokenSource.cancel();
            tokenSource.cancel();
            tokenSource.cancel();
        }
        catch (Exception e)
        {
            Assert.fail(e.getMessage());
        }

        // Then
        Assert.assertTrue(tokenSource.isCanceled());
        verify(runnable, times(1)).run();
    }

    @Test
    public void shouldNotRunWhenDettcahedHandle()
    {
        // Given
        var tokenSource = new CancellationTokenSource();
        var runnable = mock(Runnable.class);

        // When
        try (var attachToken = CancellationTokenSource.attach(tokenSource, runnable))
        {
            //
        }
        catch (Exception e)
        {
            Assert.fail(e.getMessage());
        }

        tokenSource.cancel();

        // Then
        Assert.assertTrue(tokenSource.isCanceled());
        verify(runnable, times(0)).run();
    }
}
