/**
 *
 */
package com.e1c.edt.ai.assistent.model;

import com.google.gson.annotations.SerializedName;

public class FoundElement
{
    @SerializedName("relative_file_path")
    public String relativeFilePath;

    @SerializedName("filePath")
    public String filePath;

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
