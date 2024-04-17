/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.itests;

import java.util.Arrays;
import java.util.Collection;

import org.e1c.edt.ai.URLValidator;
import org.e1c.edt.ai.ValidationError;
import org.e1c.edt.ai.ValidationResult;
import org.e1c.edt.ai.WellknownError;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class URLValidatorTest
{
    @Parameter(0)
    public String urlText;

    @Parameter(1)
    public ValidationResult expectedValidationResult;

    @Test
    @Parameters()
    public void shouldValidateText()
    {
        // Given
        var validator = new URLValidator();

        // When
        var actualValidationResult = validator.validate(urlText);

        // Then
        Assert.assertEquals(expectedValidationResult, actualValidationResult);
    }

    @SuppressWarnings("nls")
    @Parameters(name = "{index}: {0}")
    public static Collection<Object[]> data()
    {
        // @formatter:off
        return Arrays.asList(
            new Object[][] {
                {"http://abc.com", ValidationResult.Success },
                {"", new ValidationResult(UnableToParse("")) },
                {"Xyz", new ValidationResult(UnableToParse("Xyz")) },
            });
        // @formatter:on
    }

    private static ValidationError UnableToParse(String name)
    {
        return new ValidationError(WellknownError.UnableToParse, name);
    }
}
