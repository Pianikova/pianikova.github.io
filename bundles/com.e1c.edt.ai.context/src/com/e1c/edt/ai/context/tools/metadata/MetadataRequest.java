/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context.tools.metadata;

import com.google.gson.annotations.SerializedName;

final class MetadataRequest
{
    String operation;
    String topic;

    @SerializedName("project_name")
    String projectName;

    @SerializedName("object_name")
    String objectName;

    String name;

    @SerializedName("new_name")
    String newName;

    String title;

    @SerializedName("property_name")
    String propertyName;

    @SerializedName("property_value")
    String propertyValue;

    String type;
    Integer length;
    Integer precision;

    @SerializedName("date_fractions")
    String dateFractions;

    @SerializedName("field_kind")
    String fieldKind;

    @SerializedName("child_kind")
    String childKind;

    @SerializedName("subordinate_kind")
    String subordinateKind;

    @SerializedName("platform_version")
    String platformVersion;

    @SerializedName("compatibility_mode")
    String compatibilityMode;

    @SerializedName("script_variant")
    String scriptVariant;

    @SerializedName("default_language_code")
    String defaultLanguageCode;

    @SerializedName("default_language_name")
    String defaultLanguageName;

    String version;

    String vendor;

    @SerializedName("related_object_name")
    String relatedObjectName;

    @SerializedName("form_type")
    String formType;

    @SerializedName("template_type")
    String templateType;

    @SerializedName("dry_run")
    boolean dryRun;

    /**
     * Post-mutation marker auto-check. Deliberately a {@link Boolean}: Gson leaves an absent field
     * {@code null}, and {@code null} means enabled. A primitive would default to {@code false} and
     * silently disable the check whenever the model omits the parameter.
     */
    @SerializedName("verify")
    Boolean verify;

    boolean verifyEnabled()
    {
        return verify == null || verify.booleanValue();
    }
}
