/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai;

import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.google.inject.Provider;

@RunWith(Parameterized.class)
public class HashToolsTest
{
    @SuppressWarnings("unchecked")
    private static final Provider<MessageDigest> MessageDigestProvider = mock(Provider.class);

    @Parameter(0)
    public String text;

    @Parameter(1)
    public int size;

    @Parameter(2)
    public byte[] expectedBytes;

    @Test
    @Parameters()
    public void shouldScanTextStream()
    {
        // Given
        var allBytes = new ArrayList<Byte>();
        var stream = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
        var buffer = CharBuffer.allocate(size);
        var hashTools = new HashTools(MessageDigestProvider);

        // When
        try
        {
            hashTools.scanTextStream(buffer, bytes -> {
                for (var bt : bytes)
                {
                    allBytes.add(bt);
                }
            }, stream, StandardCharsets.UTF_8,
                ch -> !Character.isWhitespace(ch));
        }
        catch (Exception e)
        {
            //
        }

        // Then
        var actualBytes = new byte[allBytes.size()];
        for (var i = 0; i < allBytes.size(); i++)
        {
            actualBytes[i] = allBytes.get(i);
        }

        Assert.assertArrayEquals(expectedBytes, actualBytes);
    }

    @SuppressWarnings("nls")
    @Parameters(name = "{index} data: \"{0}\"")
    public static Collection<Object[]> data()
    {
        // @formatter:off
        return Arrays.asList(
            new Object[][] {
                { "", 1, new byte[] { } },
                { "a", 10, new byte[] { 97 } },
                { "A", 10, new byte[] { 65 } },
                { "ab", 10, new byte[] { 97, 98 } },
                { "a\tb", 10, new byte[] { 97, 98 } },
                { "a b", 10, new byte[] { 97, 98 } },
                { "a b", 10, new byte[] { 97, 98 } },
                { "a\rb", 10, new byte[] { 97, 98 } },
                { "a\nb", 10, new byte[] { 97, 98 } },
                { "\r\n a \r \tb\n  \r", 20, new byte[] { 97, 98 } },
                { "\r\n a \r \tb\n  \r", 2, new byte[] { 97, 98 } },
                { "\r\n a \r \tb\n  \r", 1, new byte[] { 97, 98 } },

            });
        // @formatter:on
    }
}
