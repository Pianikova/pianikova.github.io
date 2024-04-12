/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import org.e1c.edt.ai.ui.preferences.ClientAIPreferenceService;
import org.eclipse.swt.widgets.Display;

/**
 * @author George Suaridze
 *
 */
public class ChatAPIProvider
{
    private static ChatAPI chatAPI;

    public static ChatAPI getService()
    {
        if (chatAPI == null)
        {
            String chatURL = ClientAIPreferenceService.getSettings().getChatURL();
            // Instantiate the service within the UI thread
            Display.getDefault().syncExec(() -> {
                chatAPI = new ChatAPI(chatURL); // 10.70.2.171
            });
        }
        return chatAPI;
    }
}
