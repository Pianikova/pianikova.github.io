/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import java.net.MalformedURLException;
import java.net.URL;

import com.google.common.base.Preconditions;

public class URLValidator
    implements IValidator<String>
{
    @Override
    public ValidationResult validate(String target)
    {
        Preconditions.checkNotNull(target);
        var validationResult = new ValidationResult();
        try
        {
            new URL(target);
        }
        catch (MalformedURLException e)
        {
            validationResult.addError(new ValidationError(WellknownError.UnableToParse, target));
        }

        return validationResult;
    }
}
