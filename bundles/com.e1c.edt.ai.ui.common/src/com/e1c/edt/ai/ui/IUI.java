/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.util.Optional;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewPart;

public interface IUI
{
    Optional<Shell> getShell();

    Optional<StyledText> getTextWidget();

    Optional<SourceViewer> getLastSourceViewer();

    Optional<SourceViewer> getSourceViewer(StyledText textWidget);

    Optional<IFile> getFile(SourceViewer sourceViewer);

    /**
     * Returns the file of the currently active editor, or the last focused editor's file when called
     * off the UI thread. Empty when no editor is/was active.
     */
    Optional<IFile> getActiveFile();

    Optional<IViewPart> showView(String viewId);
}