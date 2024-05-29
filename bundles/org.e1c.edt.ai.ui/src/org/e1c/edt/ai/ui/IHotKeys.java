/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import org.eclipse.swt.events.KeyEvent;

public interface IHotKeys
{
    public static String PREFIX = "org.e1c.edt.ai.ui.commands."; //$NON-NLS-1$
    public static String SUGGEST = PREFIX + "suggest.ai"; //$NON-NLS-1$
    public static String ACCEPT = PREFIX + "accept.ai"; //$NON-NLS-1$
    public static String ACCEPT_PART = PREFIX + "acceptpart.ai"; //$NON-NLS-1$
    public static String ROLLBACK_PART = PREFIX + "rollbackpart.ai"; //$NON-NLS-1$
    public static String STOP = PREFIX + "stop.ai"; //$NON-NLS-1$

    boolean isTriggered(String bindingId, KeyEvent event);
}
