/**
 * Copyright (C) 2026, 1C
 */
package com.e1c.edt.ai.assistent.model;

import com.google.gson.annotations.SerializedName;

/**
 * A top-level window (shell) visible to the user: the workbench window or an open dialog.
 */
public class VisualWindow
    extends VisualContext
{
    @SerializedName("is_active")
    public Boolean isActive;

    @SerializedName("is_modal")
    public Boolean isModal;

    @SerializedName("is_dialog")
    public Boolean isDialog;
}
