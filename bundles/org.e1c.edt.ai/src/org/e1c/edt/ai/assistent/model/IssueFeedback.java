/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent.model;

import java.util.Map;

import com.google.gson.annotations.SerializedName;

public class IssueFeedback
{
    @SerializedName("issue_type")
    public IssueType issueType;

    @SerializedName("issue_description")
    public String issueDescription;

    @SerializedName("request_uuid")
    public String requestUuid;

    @SerializedName("meta_info")
    public Map<String, String> metaInfo;
}
