/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

/**
 * Centralized constants for MCP tool implementations.
 * This class contains magic numbers and default values used across various MCP tools.
 */
public final class McpToolConstants
{
    // File operation constants
    /** Maximum number of lines that can be read from a file */
    public static final int MAX_READ_LINES = 1000;

    /** Default number of lines to read when not specified */
    public static final int DEFAULT_READ_LINES = 500;

    // Search and result constants
    /** Default maximum number of elements for search results */
    public static final int DEFAULT_MAX_SEARCH_ELEMENTS = 32;

    /** Default maximum number of markers to return */
    public static final int DEFAULT_MAX_MARKERS = 32;

    /** Default maximum number of files to search */
    public static final int DEFAULT_MAX_FILES = 32;

    /** Default maximum number of lines for execute command output */
    public static final int DEFAULT_MAX_EXECUTION_LINES = 500;

    // Git operation constants
    /** Default maximum number of git commits to retrieve */
    public static final int DEFAULT_MAX_GIT_COMMITS = 32;

    /** Default number of context lines for git diff */
    public static final int DEFAULT_GIT_DIFF_CONTEXT_LINES = 3;

    // History operation constants
    /** Default maximum number of local history entries */
    public static final int DEFAULT_MAX_HISTORY_ENTRIES = 20;

    /** Default maximum number of navigation history entries */
    public static final int DEFAULT_MAX_NAVIGATION_ENTRIES = 20;
}
