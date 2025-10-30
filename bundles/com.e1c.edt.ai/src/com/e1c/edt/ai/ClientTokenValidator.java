/**
 *
 */
package com.e1c.edt.ai;

public class ClientTokenValidator implements IClientTokenValidator
{
    @Override
    public boolean isValid(String token)
    {
        if (token == null || token.isBlank())
        {
            return false;
        }

        for (int i = 0; i < token.length(); i++)
        {
            char c = token.charAt(i);
            if (Character.isDigit(c))
            {
                continue;
            }

            if (Character.isLetterOrDigit(c))
            {
                continue;
            }

            if (c == '-' || c == '_')
            {
                continue;
            }

            return false;
        }

        return true;
    }
}
