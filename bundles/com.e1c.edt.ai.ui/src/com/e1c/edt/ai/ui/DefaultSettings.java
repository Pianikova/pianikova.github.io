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
        return "https://code.1c.ai/"; //$NON-NLS-1$
    }

    @Override
    public String getChatUrl()
    {
        return "https://code.1c.ai/chat/"; //$NON-NLS-1$
    }

    @Override
    public String getHomePage()
    {
        return "https://code.1c.ai/"; //$NON-NLS-1$
    }

    @Override
    public String getUpdateUrl()
    {
        return "https://code.1c.ai/plugin/"; //$NON-NLS-1$
    }

    @Override
    public String getPluginFeature()
    {
        return "com.e1c.edt.ai.feature.feature.group"; //$NON-NLS-1$
    }
}
