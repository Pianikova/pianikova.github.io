/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent.model;

import java.util.Map;

import com.google.gson.annotations.SerializedName;

// {
/**
 * Элемент глобального или локального контекста.
 *
 * @author Dmitriy Marmyshev
 */
public class GlobalContext
{
    /**
     * Хэш формы.
     */
    @SerializedName("form")
    public String formHash;

    /**
     * Хэш метаданных.
     */
    @SerializedName("meta")
    public String metaHash;

    /**
     * Хэш модуля.
     */
    @SerializedName("module")
    public String moduleHash;

    /**
     * Словарь локальных методов модуля. Имя метода - хэш метода.
     */
    @SerializedName("local_functions")
    public Map<String, String> localFunctions;
    // }
    public transient String formPath;

    public transient String metaPath;

    public transient String modulePath;

    public transient Map<String, HashedValue<Object>> localFunctionsEntities;

    public transient Object formEntity;

    public transient Object metaEntity;
// {
}
// }
