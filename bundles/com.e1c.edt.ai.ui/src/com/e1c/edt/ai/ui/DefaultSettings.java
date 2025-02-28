/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.IDefaultSettings;

class DefaultSettings
    implements IDefaultSettings
{
    @Override
    public String getUrl()
    {
        return "https://code.1c.ai/api/v1/"; //$NON-NLS-1$
    }
}
