/**
 *
 */
package com.e1c.edt.ai.assistent.model;

import java.util.Map;

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
     * Принадлежность конфигурации: 'Native' или 'Adopted' (на полной поддержке поставщика,
     * изменение запрещено). Заполняется только для обычных конфигураций, не для расширений.
     */
    @SerializedName("object_belonging")
    public String objectBelonging;

    /**
     * Вариант языка 'English' или 'Russian'.
     */
    @SerializedName("script_language")
    public String scriptLanguage;

    /**
     * Версия конфигурации, например  1.2.3.
     */
    @SerializedName("version")
    public String version;

    /**
     * Версия платформы, например 8.3.24.
     */
    @SerializedName("platform_version")
    public String platformVersion;

    /**
     * Поставщик конфигурации.
     */
    @SerializedName("vendor")
    public String vendor;

    /**
     * Версия платформы, например 8.3.24.
     */
    @SerializedName("compatibility")
    public String compatibility;

    /**
     * Комментарий к конфигурации.
     */
    @SerializedName("comment")
    public String comment;

    /**
     * Краткая информация о конфигурации.
     */
    @SerializedName("brief_information")
    public Map<String, String> briefInformation;

    /**
     * Родительский проект.
     */
    @SerializedName("parent_project")
    public String parentProject;
}
