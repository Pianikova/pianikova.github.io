/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.Optional;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.IViewPart;
import org.eclipse.xtext.ui.editor.XtextEditor;

public interface IUI
{
    Optional<IViewPart> showView(String viewId);

    Optional<XtextEditor> getEditor();

    Optional<ITextSelection> getSelection();

    void select(ITextSelection selection);
}
