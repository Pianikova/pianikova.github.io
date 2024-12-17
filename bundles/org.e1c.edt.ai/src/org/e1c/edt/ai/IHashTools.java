/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import java.security.MessageDigest;

public interface IHashTools
{
    MessageDigest clone(MessageDigest hash);

    String format(MessageDigest hash);
}
