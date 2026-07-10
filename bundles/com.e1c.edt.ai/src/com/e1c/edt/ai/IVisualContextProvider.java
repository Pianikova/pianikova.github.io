/**
 *
 */
package com.e1c.edt.ai;

import com.e1c.edt.ai.assistent.model.VisualContext;
import com.e1c.edt.ai.assistent.model.VisualSnapshot;

public interface IVisualContextProvider
{
    VisualContext create(Object control, ICancellationToken cancellationToken);

    /**
     * Captures everything the user currently sees in the IDE: all open windows/dialogs,
     * the active editor viewport and the clipboard. Safe to call from any thread.
     */
    VisualSnapshot createSnapshot(ICancellationToken cancellationToken);
}
