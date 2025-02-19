/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ui.eclipse;

import org.e1c.edt.ai.IDefaultSettings;

class DefaultSettings
    implements IDefaultSettings
{
    @Override
    public String getUrl()
    {
        return "https://llms.1c.ai/code_java/api/v1/"; //$NON-NLS-1$
    }
}
