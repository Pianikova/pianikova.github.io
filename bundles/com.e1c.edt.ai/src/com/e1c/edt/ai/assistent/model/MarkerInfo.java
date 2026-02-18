/**
 *
 */
package com.e1c.edt.ai.assistent.model;

import java.util.Map;

import com.google.gson.annotations.SerializedName;

public class MarkerInfo
{
    // Severity constants
    /** Severity value for error markers */
    public static final String SEVERITY_ERROR = "error"; //$NON-NLS-1$

    /** Severity value for warning markers */
    public static final String SEVERITY_WARNING = "warning"; //$NON-NLS-1$

    /** Severity value for info markers */
    public static final String SEVERITY_INFO = "info"; //$NON-NLS-1$

    // Priority constants
    /** Priority value for high priority markers */
    public static final String PRIORITY_HIGH = "high"; //$NON-NLS-1$

    /** Priority value for normal priority markers */
    public static final String PRIORITY_NORMAL = "normal"; //$NON-NLS-1$

    /** Priority value for low priority markers */
    public static final String PRIORITY_LOW = "low"; //$NON-NLS-1$

    @SerializedName("path")
    public String path;

    @SerializedName("start_line")
    public Integer startLine;

    @SerializedName("message")
    public String message;

    @SerializedName("type")
    public String type;

    @SerializedName("severity")
    public String severity;

    @SerializedName("priority")
    public String priority;

    @SerializedName("done")
    public Boolean done;

    @SerializedName("location")
    public String location;

    @SerializedName("marker_highlighted_text")
    public String markerHighlightedText;

    @SerializedName("source_id")
    public String sourceId;

    @SerializedName("details")
    public Map<String, Object> details;
}
