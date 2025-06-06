/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent.model;

public class HashedValue<T>
{
    public final T Value;
    public final String Hash;

    public HashedValue(T value, String hash)
    {
        Value = value;
        Hash = hash;
    }
}
