/**
 *
 */
package com.e1c.edt.ai.assistent.model;

import com.google.gson.annotations.SerializedName;

public class FoundElement
{
    /**
     * Name of the project
     */
    @SerializedName("project_name")
    public String projectName;

    /**
     * Project relative path to the file.
     */
    @SerializedName("relative_file_path")
    public String relativeFilePath;

    @SerializedName("absolute_file_path")
    public String absoluteFilePath;

    public int offset;

    public int length;

    @SerializedName("line_offset")
    public int lineOffset;

    @SerializedName("line_length")
    public int lineLength;

    @SerializedName("line_number")
    public int lineNumber;

    @SerializedName("line_contents")
    public String lineContents;
}
