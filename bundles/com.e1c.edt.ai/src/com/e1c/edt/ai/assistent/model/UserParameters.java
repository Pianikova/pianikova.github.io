/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent.model;

import com.google.gson.annotations.SerializedName;

/**
 * Параметры пользователя.
 */
public class UserParameters
{
    /**
     * Версия EDT. Например, 1.0.0.0.
     */
    @SerializedName("edt_version")
    public String edtVersion;

    /**
     * Версия плагина. Например, 1.0.0.0.
     */
    @SerializedName("plugin_version")
    public String pluginVersion;

    /**
     * Количество пробелов в отступах. Например, 4.
     */
    @SerializedName("tab_width")
    public int tabWidth;

    /**
     * Количество строк завершения кода. Например, 5.
     */
    @SerializedName("code_completion_lines_count")
    public int codeCompletionLinesCount;

    /**
     * Политика завершения кода off/focusing/balance/creativity. Например, balance.
     *
     * @see CodeCompletionPolicy
     */
    @SerializedName("code_completion_policy")
    public CodeCompletionPolicy codeCompletionPolicy;

    /**
     * Минимальная задержка запроса для завершения кода.
     */
    @SerializedName("min_request_delay_ms")
    public long minRequestDelayMs;

    /**
     * Таймаут запроса милисекунд. Например, 10000.
     */
    @SerializedName("timeout_ms")
    public long timeoutMs;

    /**
     * Разделитель строк. Например, "\r\n".
     */
    @SerializedName("line_separator")
    public String lineSeparator;

    /**
     * Отправлять ли расширенный контекст. Например, true.
     */
    @SerializedName("send_context")
    public boolean sendContext;

    /**
     * Язык интерфейса. Например, "Russian".
     */
    @SerializedName("language")
    public String language;
}
