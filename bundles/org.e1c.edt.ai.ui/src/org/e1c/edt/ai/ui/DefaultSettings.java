/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai.ui;

import org.e1c.edt.ai.IDefaultSettings;

class DefaultSettings
    implements IDefaultSettings
{
    @Override
    public String getUrl()
    {
        return "https://llms.1c.ai/code/api/v1/"; //$NON-NLS-1$
    }
}
