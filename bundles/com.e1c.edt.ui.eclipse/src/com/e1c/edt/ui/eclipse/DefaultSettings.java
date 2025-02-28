/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ui.eclipse;

import com.e1c.edt.ai.IDefaultSettings;

class DefaultSettings
    implements IDefaultSettings
{
    @Override
    public String getUrl()
    {
        return "https://llms.1c.ai/code_java/api/v1/"; //$NON-NLS-1$
    }
}
