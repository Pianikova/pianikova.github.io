/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.assistent.model;

import com.google.gson.annotations.SerializedName;

/**
 * Error response from session API.
 *
 * @author Bogdan Sushkov
 *
 */
public class SessionErrorResponse
{
    /**
     * Error message.
     */
    public String error;

    /**
     * Error type.
     */
    @SerializedName("error_type")
    public String errorType;

}
