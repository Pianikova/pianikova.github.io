/**
 * Copyright (C) 2026, 1C
 */
package com.e1c.edt.ai.assistent.model;

import java.util.List;

import com.google.gson.annotations.SerializedName;

/**
 * Everything the user currently sees in the IDE: all open windows/dialogs (the active one first),
 * the active editor viewport and the clipboard.
 */
public class VisualSnapshot
{
    @SerializedName("windows")
    public List<VisualWindow> windows;

    @SerializedName("active_editor")
    public VisualEditorInfo activeEditor;

    @SerializedName("clipboard")
    public ClipboardInfo clipboard;

    public boolean isEmpty()
    {
        return (windows == null || windows.isEmpty()) && activeEditor == null && clipboard == null;
    }
}
