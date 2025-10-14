/**
 *
 */
package com.e1c.edt.ui.eclipse;

import com.e1c.edt.ai.ui.IClientTokenValidator;

public class ClientTokenValidator
    implements IClientTokenValidator
{
    @Override
    public boolean isValid(String token)
    {
        return true;
    }
}
