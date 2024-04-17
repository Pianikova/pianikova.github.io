/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import org.eclipse.jface.text.IDocument;

public interface ICodeAssistentText
{

    String get(IDocument document, int cursorOffset);

    void set(IDocument document, int cursorOffset, String text);
}
