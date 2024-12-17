/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import java.security.MessageDigest;

public class HashTools
    implements IHashTools
{
    @Override
    public MessageDigest clone(MessageDigest hash)
    {
        try
        {
            return (MessageDigest)hash.clone();
        }
        catch (CloneNotSupportedException e)
        {
            return hash;
        }
    }

    @Override
    public String format(MessageDigest hash)
    {
        var bytes = clone(hash).digest();
        var text = new StringBuilder(hash.getAlgorithm().length() + 1 + bytes.length * 2);
        text.append(hash.getAlgorithm());
        text.append(':');

        for (var i = 0; i < bytes.length; i++)
        {
            var bt = bytes[i];
            if ((0xff & bt) < 0x10)
            {
                text.append('0');
            }

            text.append(Integer.toHexString(0xFF & bt));
        }

        return text.toString();
    }
}
