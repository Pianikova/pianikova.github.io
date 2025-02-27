/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai;

public interface IValidator<TTarget>
{
    ValidationResult validate(TTarget target);
}
