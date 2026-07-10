/**
 *
 */
package com.e1c.edt.ai.assistent.model;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class VisualField
{
    public String name;

    public String value;

    @SerializedName("is_focused")
    public Boolean isFocused;

    @SerializedName("is_multiline")
    public Boolean isMultiline;

    /* Control kind: "text", "combo", "checkbox", "radio", "button", "link", "list", "table", "tree", "tabs". */
    @SerializedName("kind")
    public String kind;

    @SerializedName("is_checked")
    public Boolean isChecked;

    @SerializedName("is_enabled")
    public Boolean isEnabled;

    /* Combo/list items or tab titles. */
    @SerializedName("options")
    public List<String> options;

    /* Text selected by the user in this control. */
    @SerializedName("selected_text")
    public String selectedText;

    /* Table/tree column headers. */
    @SerializedName("columns")
    public List<String> columns;

    /* Table/tree rows visible to the user (viewport only). */
    @SerializedName("rows")
    public List<List<String>> rows;

    /* Set when the captured value/rows/options were truncated by the capture budget. */
    @SerializedName("is_truncated")
    public Boolean isTruncated;
}
