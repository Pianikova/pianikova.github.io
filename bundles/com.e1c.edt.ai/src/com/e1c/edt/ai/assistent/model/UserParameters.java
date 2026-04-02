/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent.model;

import java.util.Optional;

import com.google.gson.annotations.SerializedName;

// {
/**
 * Параметры пользователя.
 */
public class UserParameters
{
    /**
     * Версия EDT. Например, 2025.6.0.
     */
    @SerializedName("edt_version")
    public String edtVersion;

    /**
     * Версия плагина. Например, 1.0.4
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
     * Политика завершения кода off/focusing/balance/creativity. Например, moderate.
     *
     * @see CodeCompletionPolicy
     */
    @SerializedName("code_completion_policy")
    public CodeCompletionPolicy codeCompletionPolicy;

    /**
     * Определяет включено ли автоматическое продолжение кода. Например, true.
     */
    @SerializedName("is_continuous_code_completion")
    public Boolean isContinuousCodeCompletion = true;

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
     * Язык интерфейса. Например, "Russian".
     */
    @SerializedName("language")
    public String language;

    /**
     * Определяет передавать ли глобальный контекст. Например, true.
     */
    @SerializedName("global_context")
    public Optional<Boolean> globalContext;

    /**
     * Определяет использовать ли экспериментальные возможности. Например, true.
     */
    public Optional<Boolean> experimental;

    /**
     * Уровень детализации логов (error/warning/info/trace/debug). Например, warning.
     */
    public Verbosity verbosity;

    /**
     * Переопределяет пут к ресурсам. Например, "C:/Users/user/resources".
     */
    @SerializedName("resources")
    public String resources;

    /**
     * Переопределяет размер контекста для git diff. Например, 16.
     */
    @SerializedName("git_diff_context_lines")
    public Integer gitDiffContextLines;

    /**
     * Параметры проекта.
     */
    @SerializedName("configuration_parameters")
    public ConfigurationParameters configurationParameters;
}
// }

