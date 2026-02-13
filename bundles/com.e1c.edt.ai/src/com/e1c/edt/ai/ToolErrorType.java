/**
 *
 */
package com.e1c.edt.ai;

/**
 * Enum representing error types for MCP tool operations with LLM-focused categorization.
 * <p>
 * Error types are categorized based on how LLM should handle them:
 * <ul>
 * <li>{@link #RETRYABLE} - LLM should silently correct parameters and retry without user notification</li>
 * <li>{@link #USER_VISIBLE} - LLM should inform user about the objective limitation and adjust approach</li>
 * </ul>
 */
public enum ToolErrorType
{
    /**
     * Retryable errors that require LLM to silently correct parameters and retry.
     * <p>
     * These errors indicate issues with how LLM called the tool - incorrect parameters,
     * invalid format, or logic errors that LLM can fix itself. The user should NOT see these.
     * <p>
     * Examples:
     * <ul>
     * <li>Invalid JSON format or structure</li>
     * <li>Missing required parameters (path, content, etc.)</li>
     * <li>Invalid parameter values (negative line numbers, wrong data types)</li>
     * <li>Multiple matches found when single replacement expected</li>
     * <li>Original content not found for replacement</li>
     * <li>Invalid marker type specified</li>
     * <li>Validation errors LLM can fix by adjusting call parameters</li>
     * </ul>
     * <p>
     * Expected LLM behavior:
     * <ul>
     * <li>Analyze the error message</li>
     * <li>Correct the call parameters</li>
     * <li>Retry the tool call WITHOUT informing the user</li>
     * </ul>
     */
    RETRYABLE("retryable", "Retryable error - LLM should silently correct and retry"),

    /**
     * User-visible errors indicating objective limitations that prevent operation execution.
     * <p>
     * These errors indicate that the operation cannot be performed due to external constraints
     * (file doesn't exist, project closed, etc.). LLM should inform the user and adjust its approach.
     * <p>
     * Examples:
     * <ul>
     * <li>Project not found or does not exist</li>
     * <li>Project is closed and cannot be opened</li>
     * <li>File not found in project context</li>
     * <li>File already exists (for Write operation)</li>
     * <li>File cannot be edited (locked or unsupported type)</li>
     * <li>Access denied or permission restrictions</li>
     * <li>Operation cancelled by user</li>
     * <li>I/O errors (failed to read/write file)</li>
     * <li>Workspace or Eclipse operation failures</li>
     * <li>Build operation errors</li>
     * <li>Unsupported encoding</li>
     * </ul>
     * <p>
     * Expected LLM behavior:
     * <ul>
     * <li>Inform the user about the limitation</li>
     * <li>Explain why the operation cannot be performed</li>
     * <li>Suggest alternative approaches if possible</li>
     * <li>Adjust the plan based on the limitation</li>
     * </ul>
     */
    USER_VISIBLE("user_visible", "User-visible error - LLM should inform user and adjust approach");

    private final String code;
    private final String description;

    /**
     * Constructor for ToolErrorType enum.
     *
     * @param code the error code (machine-readable identifier)
     * @param description human-readable description of the error type
     */
    ToolErrorType(String code, String description)
    {
        this.code = code;
        this.description = description;
    }

    /**
     * Returns the error code for this error type.
     * <p>
     * The code is a machine-readable identifier that can be used for
     * programmatic error handling and logging.
     *
     * @return the error code
     */
    public String getCode()
    {
        return code;
    }

    /**
     * Returns the human-readable description of this error type.
     * <p>
     * The description provides a clear explanation of what this error
     * type represents, suitable for displaying to end users or for
     * documentation purposes.
     *
     * @return the error description
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * Returns the error type for the given error code.
     * <p>
     * This is a convenience method for looking up error types by their
     * machine-readable code.
     *
     * @param code the error code to look up
     * @return the corresponding ToolErrorType, or null if not found
     */
    public static ToolErrorType fromCode(String code)
    {
        if (code == null)
        {
            return null;
        }

        for (ToolErrorType type : values())
        {
            if (type.code.equalsIgnoreCase(code))
            {
                return type;
            }
        }

        return null;
    }
}