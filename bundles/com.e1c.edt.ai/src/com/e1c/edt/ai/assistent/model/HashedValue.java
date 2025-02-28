/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent.model;

import java.security.MessageDigest;

public class HashedValue<T>
{
    public final T Value;
    public final MessageDigest Hash;

    public HashedValue(T value, MessageDigest hash)
    {
        Value = value;
        Hash = hash;
    }
}
