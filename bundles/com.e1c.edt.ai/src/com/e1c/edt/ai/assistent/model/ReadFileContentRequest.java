/**
 *
 */
package com.e1c.edt.ai.assistent.model;

import com.google.gson.annotations.SerializedName;

public class ReadFileContentRequest
{
    /**
     * Project name in IDE.
     */
    @SerializedName("project_name")
    public String projectName;

    /**
     * Relative path to the file. For example, "/src/MyModule.bsl".
     */
    @SerializedName("relative_file_path")
    public String relativeFilePath;

    /**
     * Number of the first line of the file to be read. Numbering starts at 0. The default is 0.
     */
    @SerializedName("first_line_number")
    public Integer firstLineNumber;

    /**
     * Number of lines to read. By default, reads up to 2000 lines.
     */
    @SerializedName("lines_number")
    public Integer linesNumber;
}
