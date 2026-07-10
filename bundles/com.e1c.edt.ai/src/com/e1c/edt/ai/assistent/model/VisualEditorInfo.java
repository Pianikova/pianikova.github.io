/**
 * Copyright (C) 2026, 1C
 */
package com.e1c.edt.ai.assistent.model;

import com.google.gson.annotations.SerializedName;

/**
 * The active editor as the user sees it: only the viewport content, never the whole document.
 */
public class VisualEditorInfo
{
    @SerializedName("title")
    public String title;

    @SerializedName("path")
    public String path;

    @SerializedName("is_dirty")
    public Boolean isDirty;

    /* The part of the document currently visible in the editor viewport. */
    @SerializedName("visible_text")
    public String visibleText;

    @SerializedName("selected_text")
    public String selectedText;

    /* Caret position, 1-based. */
    @SerializedName("cursor_line")
    public Integer cursorLine;

    @SerializedName("cursor_column")
    public Integer cursorColumn;
}
