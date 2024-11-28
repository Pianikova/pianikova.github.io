/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.Optional;

import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewPart;

public interface IUI
{
    Optional<Shell> getShell();

    Optional<StyledText> getTextWidget();

    Optional<SourceViewer> getSourceViewer(StyledText textWidget);

    Optional<IViewPart> showView(String viewId);
}