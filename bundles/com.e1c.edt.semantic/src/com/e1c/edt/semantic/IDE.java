/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.semantic;

import java.util.Optional;

import com.e1c.edt.ai.ICancellationToken;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

class IDE
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
