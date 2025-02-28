/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.util.Optional;

import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewPart;

public interface IUI
{
    Optional<Shell> getShell();

    Optional<StyledText> getTextWidget();

    Optional<SourceViewer> getSourceViewer(StyledText textWidget);

    Optional<IViewPart> showView(String viewId);

    Optional<IEditorPart> getEditor(ISourceViewer sourceViewer);
}