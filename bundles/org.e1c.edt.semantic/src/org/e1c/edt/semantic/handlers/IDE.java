/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.semantic.handlers;

import java.util.Optional;

import org.e1c.edt.ai.ICancellationToken;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

public class IDE
    implements IIDE
{
    @Override
    public Optional<CloseResponse> close(CloseRequest request, ICancellationToken cancellationToken)
    {
        Display.getDefault().asyncExec(() -> {
            PlatformUI.getWorkbench().close();
        });

        return Optional.of(new CloseResponse());
    }
}
