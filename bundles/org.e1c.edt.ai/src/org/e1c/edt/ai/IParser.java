/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

public interface IParser<TTarget, TResult>
{
    TResult parse(TTarget target);
}
