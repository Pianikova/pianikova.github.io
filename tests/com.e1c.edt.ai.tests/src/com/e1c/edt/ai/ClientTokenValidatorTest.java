/**
 *
 */
package com.e1c.edt.ai;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ClientTokenValidatorTest
{
    @Parameter(0)
    public String token;

    @Parameter(1)
    public boolean expectedResult;

    @Test
    @Parameters()
    public void shouldValidate()
    {
        // Given
        var validator = createInstance();

        // When
        var actualResult = validator.isValid(token);

        // Then
        Assert.assertEquals(expectedResult, actualResult);
    }

    private ClientTokenValidator createInstance()
    {
        return new ClientTokenValidator();
    }

    @SuppressWarnings("nls")
    @Parameters(name = "{index}: {0}")
    public static Collection<Object[]> data()
    {
        // @formatter:off
        return Arrays.asList(
            new Object[][] {
                { "", false },
                { "   ", false },
                { "09c9d79b24304a728c50cd593f00fc07", true },
                { "XbYYj3ZHIHd-3fr32Yg_AvMA5BimyWKU8-BCtgQYy_Y", true },
                { "XbYYj3ZHIHd-3fr32Yg_AvMA5BimyWKU8-BCtgQYy_Y  ", false },
                { "    XbYYj3ZHIHd-3fr32Yg_AvMA5BimyWKU8-BCtgQYy_Y  ", false },
                { " XbYYj3ZHIHd-3fr32Yg_AvMA5BimyWKU8-BCtgQYy_Y", false },
                { "XbYYj3ZHIHd-3fr32Yg_AvMA5BimyWKU8-BCtgQYy_ Y", false },
            });
     // @formatter:on
    }
}
