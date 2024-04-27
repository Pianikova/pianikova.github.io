/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import org.e1c.edt.ai.ILog;
import org.eclipse.jface.text.BadLocationException;

public class IdeApiHandler
{
    private ILog log;
    private IUI ui;

    public IdeApiHandler(ILog log, IUI ui)
    {
        this.log = log;
        this.ui = ui;
    }

    public void wink(String parameter)
    {
        System.out.println("Winked: " + parameter); //$NON-NLS-1$
    }

    public void paste_code(String code)
    {
        ui.getEditor().ifPresent(editor -> ui.getSelection().ifPresent(selection -> {
            try
            {
                editor.getDocument().replace(selection.getOffset(), selection.getLength(), code);
            }
            catch (BadLocationException e)
            {
                log.logError(e);
            }
        }));
    }
}
