/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent.model;

import com.google.gson.annotations.SerializedName;

// {
/**
 * Информация о принятом фрагменте кода.
 */
public class AcceptedCodeFeedback
{
    /**
     * Принятый фрагмент кода.
     */
    @SerializedName("accepted_code")
    public String acceptedCode;

    /**
     * Информация о начальной позиции фрагмента кода.
     */
    @SerializedName("cursor_start_info")
    public CursorInfo cursorStartInfo;

    /**
     * Информация о конечной позиции фрагмента кода.
     */
    @SerializedName("cursor_end_info")
    public CursorInfo cursorEndInfo;

    /**
     * Идентификатор запроса на продолжение кода, связанного с принятым фрагментом кода.
     */
    @SerializedName("request_uuid")
    public String requestUuid;
}
// }
