/**
 *
 */
package com.e1c.edt.ai.ui;

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

            if (((c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f')))
            {
                continue;
            }

            return false;
        }

        return true;
    }
}
