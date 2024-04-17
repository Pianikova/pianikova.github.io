/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import java.net.MalformedURLException;
import java.net.URL;

public class URLValidator
    implements IValidator<String>
{

    @Override
    public ValidationResult validate(String target)
    {
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
