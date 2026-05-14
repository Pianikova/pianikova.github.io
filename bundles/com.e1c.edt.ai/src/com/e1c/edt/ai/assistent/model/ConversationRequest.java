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
    /**
     * Имя инструмента. Возможные варианты:
     * <ul>
     *  <li>docstring</li>
     *  <li>explain</li>
     *  <li>review</li>
     *  <li>modify</li>
     *  <li>custom</li>
     *  <li>system</li>
     *  <li>raw</li>
     * </ul>
     */
    @SerializedName("skill_name")
    public String skillName;

    /**
     * Язык интерфейса (русский или английский)
     */
    @SerializedName("ui_language")
    public String uiLanguage;
    @SerializedName("programming_language")
    public String programmingLanguage;
    @SerializedName("script_language")
    public String scriptLanguage;


    /**
     * Ведётся ли диалог в чате (всегда <code>true</code>)
     */
    @SerializedName("is_chat")
    public boolean isChat = true;
}
