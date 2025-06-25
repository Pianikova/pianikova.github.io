/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context.DTO;

import com.google.gson.annotations.SerializedName;

public enum ValueType
{
    @SerializedName("null")
    /**
     * Null value.
     */
    NULL,

    @SerializedName("undefined")
    /**
     * Undefined value.
     */
    UNDEFINED,

    @SerializedName("unknown")
    /**
     * Unknown value.
     */
    UNKNOWN,

    @SerializedName("boolean")
    /**
     * Boolean value.
     */
    BOOLEAN,

    @SerializedName("integer")
    /**
     * Integer value.
     */
    INTEGER,

    @SerializedName("decimal")
    /**
     * Decimal value.
     */
    DECIMAL,

    @SerializedName("string")
    /**
     * String value.
     */
    STRING,

    @SerializedName("datetime")
    /**
     * Datetime value.
     */
    DATETIME,

    @SerializedName("binary")
    /**
     * Binary value.
     */
    BINARY,

    @SerializedName("reference")
    /**
     * Reference value.
     */
    REFERENCE,

    @SerializedName("irresolvable_reference")
    /**
     * Irresolvable reference value.
     */
    IRRESORVABLE_REFERENCE,

    @SerializedName("list")
    /**
     * List value.
     */
    LIST,

    @SerializedName("array")
    /**
     * Array value.
     */
    ARRAY,

    @SerializedName("type")
    /**
     * Type value.
     */
    TYPE,

    @SerializedName("standard_period")
    /**
     * Standard period value.
     */
    STANDARD_PERIOD,

    @SerializedName("border")
    /**
     * Border value.
     */
    BORDER,

    @SerializedName("color")
    /**
     * Color value.
     */
    COLOR,

    @SerializedName("font")
    /**
     * Font value.
     */
    FONT,

    @SerializedName("account_type")
    /**
     * Account type value.
     */
    ACCOUNT_TYPE,

    @SerializedName("chart_line_type")
    /**
     * Chart line type value.
     */
    CHART_LINE_TYPE,

    @SerializedName("enum")
    /**
     * Enum value.
     */
    ENUM,

    @SerializedName("sys_enum")
    /**
     * System enum value.
     */
    SYS_ENUM,

    @SerializedName("form_choice_list_des_time")
    /**
     * Form choice list description time value.
     */
    FORM_CHOICE_LIST_DES_TIME
}
