/**
 *
 */
package com.e1c.edt.ai.assistent.model;

import com.google.gson.annotations.SerializedName;

public class WriteFileContentRequest
{
    /**
     * Project name in IDE.
     */
    @SerializedName("project_name")
    public String projectName;

    /**
     * Relative path to the file. Must start with the project name, for example, "/src/MyModule.bsl".
     */
    @SerializedName("relative_file_path")
    public String relativeFilePath;

    /**
     * Contents to write to file.
     */
    @SerializedName("contents")
    public String contents;

    /**
     * File encoding, for example, "UTF-8", "windows-1251", "KOI8-R", "UTF-16", "UTF-32", etc. By default, UTF-8.
     */
    @SerializedName("charset_name")
    public String charsetName;
}
