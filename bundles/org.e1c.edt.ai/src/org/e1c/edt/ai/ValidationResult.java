/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.google.common.base.Preconditions;

public class ValidationResult
{
    private ArrayList<ValidationError> errors;

    public static final ValidationResult SUCCESS = new ValidationResult();

    public ValidationResult(ValidationError... errors)
    {
        Preconditions.checkNotNull(errors);
        this.errors = new ArrayList<>(Arrays.asList(errors));
    }

    public ValidationResult addError(ValidationError error)
    {
        Preconditions.checkNotNull(error);
        errors.add(error);
        return this;
    }

    public List<ValidationError> getErrors()
    {
        return Collections.unmodifiableList(errors);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(errors);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ValidationResult other = (ValidationResult)obj;
        return Objects.equals(errors, other.errors);
    }

}
