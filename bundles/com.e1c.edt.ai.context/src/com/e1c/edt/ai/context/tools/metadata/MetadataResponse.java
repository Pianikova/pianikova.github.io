/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context.tools.metadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.annotations.SerializedName;

final class MetadataResponse
{
    boolean success;
    String operation;

    @SerializedName("project_name")
    String projectName;

    String target;

    @SerializedName("resource_path")
    String resourcePath;

    @SerializedName("marker_path")
    String markerPath;

    @SerializedName("artifact_path")
    String artifactPath;

    Map<String, Object> details;

    boolean changed;

    @SerializedName("dry_run")
    boolean dryRun;

    String code;
    String message;

    @SerializedName("invalid_parameter")
    String invalidParameter;

    @SerializedName("valid_values")
    List<String> validValues;

    @SerializedName("help_topic")
    String helpTopic;

    List<String> warnings = new ArrayList<>();

    static MetadataResponse success(MetadataRequest request, String target, boolean changed)
    {
        var response = new MetadataResponse();
        response.success = true;
        response.operation = request.operation;
        response.projectName = request.projectName;
        response.target = target;
        response.changed = changed;
        response.dryRun = request.dryRun;
        return response;
    }
}
