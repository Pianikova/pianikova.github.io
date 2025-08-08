/**
 *
 */
package com.e1c.edt.ai.assistent.model;

import com.google.gson.annotations.SerializedName;

public class ConversationRequest
{
    /**
     * Имя инструмента
     */
    @SerializedName("tool_name")
    public String toolName;

    /**
     * Язык интерфейса (русский или английский)
     */
    @SerializedName("ui_language")
    public String uiLanguage;

    /**
     * Язык программирования (1с, java)
     */
    @SerializedName("programming_language")
    public String programmingLanguage;

    /**
     * Язык кода (русский или английский). На каком языке комментарии к коду
     */
    @SerializedName("script_language")
    public String scriptLanguage;
}
