/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai.assistent.model;

import com.google.gson.annotations.SerializedName;

public class AcceptedCodeFeedback
{
    @SerializedName("accepted_code")
    public String acceptedCode;

    @SerializedName("cursor_start_info")
    public CursorInfo cursorStartInfo;

    @SerializedName("cursor_end_info")
    public CursorInfo cursorEndInfo;

    @SerializedName("request_uuid")
    public String requestUuid;
}
