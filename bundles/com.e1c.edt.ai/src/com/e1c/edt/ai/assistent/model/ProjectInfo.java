/**
 *
 */
package com.e1c.edt.ai.assistent.model;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class ProjectInfo
{
    @SerializedName("name")
    public String name;

    @SerializedName("absolute_path")
    public String absolutePath;

    @SerializedName("is_open")
    public Boolean isOpen;

    @SerializedName("exists")
    public Boolean exists;

    @SerializedName("is_current")
    public Boolean isCurrent;

    /**
     * List of directories in the project.
     */
    @SerializedName("directories")
    public List<String> directories;
}