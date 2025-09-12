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
        return "https://llms.1c.ai/code_java/"; //$NON-NLS-1$
    }

    @Override
    public String getChatUrl()
    {
        return "https://llms.1c.ai/code_java/chat/"; //$NON-NLS-1$
    }

    @Override
    public String getHomePage()
    {
        return "https://code.1c.ai/"; //$NON-NLS-1$
    }

    @Override
    public String getUpdateUrl()
    {
        return "jar:file:Z:/Transfer/ai_plugin/com.e1c.edt.ui.eclipse.repository.zip/!"; //$NON-NLS-1$
    }

    @Override
    public String getPluginFeature()
    {
        return "com.e1c.edt.ui.eclipse.feature.feature.group"; //$NON-NLS-1$
    }
}
