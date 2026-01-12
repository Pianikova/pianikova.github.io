/**
 *
 */
package com.e1c.edt.ai.assistent.model;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class FileFindRequest
{
    /**
     * Text or regular expression to search . The search text represents a regular expression or a pattern using '*' and '?' as wildcards. The empty search text signals a file name search.
     */
    @SerializedName("search_query")
    public String searchQuery;

    /**
     * Specifies whether the pattern should be case-sensitive. Defaults to false.
     */
    @SerializedName("is_case_sensitive_search")
    public Boolean isCaseSensitiveSearch;

    /**
     * Specifies whether the search text contains a regular expression or not. Defaults to false.
     */
    @SerializedName("is_regular_expression_search")
    public Boolean isReqularExpressionSearch;

    /**
     * IDE project names. If not specified, all projects will be searched.
     */
    @SerializedName("search_project_names")
    public List<String> projectNames;

    /**
     * Filename patterns that all files must match. If not specified, then all file names must be included.
     */
    @SerializedName("file_name_patterns")
    public List<String> fileNamePatterns;

    /**
     * True means including derived files and files inside derived containers. False means excluding them. The default value is True.
     */
    @SerializedName("include_derived")
    public Boolean includeDerived;
}
