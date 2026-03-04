/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.assistent.model;

import com.google.gson.annotations.SerializedName;


/**
 * @author Bogdan Sushkov
 *
 */
public class ConversationRequest
{
    @SerializedName("skill_name")
    public String skillName;
    @SerializedName("ui_language")
    public String uiLanguage;
    @SerializedName("programming_language")
    public String programmingLanguage;
    @SerializedName("script_language")
    public String scriptLanguage;
    @SerializedName("is_chat")
    public Boolean isChat;
}
