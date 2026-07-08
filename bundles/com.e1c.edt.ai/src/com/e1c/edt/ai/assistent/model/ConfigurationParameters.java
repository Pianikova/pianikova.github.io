/**
 *
 */
package com.e1c.edt.ai.assistent.model;

import java.util.List;
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
     * Версии платформы, поддерживаемые данной установкой EDT, по возрастанию (последняя — самая новая).
     */
    @SerializedName("available_platform_versions")
    public List<String> availablePlatformVersions;

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
