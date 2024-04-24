/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.Optional;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension2;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.IViewPart;
import org.eclipse.xtext.ui.editor.XtextEditor;

public interface IUI
{
    Optional<IViewPart> showView(String viewId);

    Optional<XtextEditor> getEditor();

    Optional<ITextViewer> getTextViewer();

    Optional<ITextViewerExtension2> getTextViewerExtension2();

    Optional<ISelectionProvider> getSelectionProvider();

    Optional<ITextSelection> getSelection();

    void select(ITextSelection selection);
}
