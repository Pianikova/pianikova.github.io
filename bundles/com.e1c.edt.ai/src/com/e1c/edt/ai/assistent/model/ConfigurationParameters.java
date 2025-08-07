/**
 *
 */
package com.e1c.edt.ai.assistent.model;

import com.google.gson.annotations.SerializedName;

public class ConfigurationParameters
{
    /**
     * Имя конфигурации.
     */
    @SerializedName("name")
    public String name;

    /**
     * Тип конфигурации 'Configuration' или 'Extension'.
     */
    @SerializedName("type")
    public String type;

    /**
     * Вариант языка 'English' или 'Russian'.
     */
    @SerializedName("script_language")
    public String scriptLanguage;

    /**
     * Версия конфигурации.
     */
    @SerializedName("version")
    public String version;

    /**
     * Поставщик конфигурации.
     */
    @SerializedName("vendor")
    public String vendor;
}
