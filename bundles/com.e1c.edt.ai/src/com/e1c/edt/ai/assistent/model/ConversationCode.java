/**
 *
 */
package com.e1c.edt.ai.assistent.model;

import com.google.gson.annotations.SerializedName;

public class ConversationCode
{
    /**
     * Команда пользователя (наличие поля зависит от tool_name)
     */
    @SerializedName("instruction")
    public String instruction;

    /**
     * Путь к файлу с кодом
     */
    @SerializedName("path")
    public String path;

    /**
     * Начальная строка кода
     */
    @SerializedName("start_line")
    public int startLine;

    /**
     *  Конечная строка кода
     */
    @SerializedName("end_line")
    public int endLine;

    /**
     * Содержимое кода
     */
    @SerializedName("content")
    public String content;
}
