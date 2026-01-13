/**
 *
 */
package com.e1c.edt.ai.assistent.model;

import com.google.gson.annotations.SerializedName;

public class EditFileContentRequest
{
    /**
     * Project name in IDE.
     */
    @SerializedName("project_name")
    public String projectName;

    /**
     * Relative path to the file which contents should be replaced. Must start with the project name, for example, "src/MyModule.bsl".
     */
    @SerializedName("relative_file_path")
    public String relativeFilePath;

    /**
     * The fragment of the file content that will be replaced.
     */
    @SerializedName("origin_contents")
    public String originContents;

    /**
     * The content fragment that will replace the original ('origin_contents').
     */
    @SerializedName("new_contents")
    public String newContents;

    /**
     * If true, all occurrences of the 'origin_contents' fragment will be replaced.
     * If false, only the single occurrence will be replaced. If no fragments are found, or more than one is found, the request will fail.
     * False by default.
     */
    @SerializedName("replace_all")
    public Boolean replaceAll;
}
