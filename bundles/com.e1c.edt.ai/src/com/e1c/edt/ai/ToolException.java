/**
 *
 */
package com.e1c.edt.ai;

/**
 * Exception thrown when an error occurs during MCP tool execution.
 * <p>
 * This exception carries information about the error type to guide LLM behavior:
 * <ul>
 * <li>{@link ToolErrorType#RETRYABLE} - LLM should silently correct parameters and retry</li>
 * <li>{@link ToolErrorType#USER_VISIBLE} - LLM should inform user and adjust approach</li>
 * </ul>
 */
public class ToolException
    extends RuntimeException
{
    private final ToolErrorType errorType;
    private final ToolCallMessage callMessage;

    /**
     * Constructs a ToolException with message, cause, and error type.
     *
     * @param message the error message
     * @param cause the underlying cause
     * @param errorType the error type indicating how LLM should handle this error
     */
    public ToolException(String message, Throwable cause, ToolErrorType errorType)
    {
        super(message, cause);
        this.errorType = errorType;
        callMessage = null;
    }

    /**
     * Constructs a ToolException with message and error type.
     *
     * @param message the error message
     * @param errorType the error type indicating how LLM should handle this error
     */
    public ToolException(String message, ToolErrorType errorType)
    {
        super(message);
        this.errorType = errorType;
        callMessage = null;
    }

    /**
     * Constructs a ToolException with only error type.
     *
     * @param errorType the error type indicating how LLM should handle this error
     */
    public ToolException(ToolErrorType errorType)
    {
        this.errorType = errorType;
        callMessage = null;
    }

    /**
     * Constructs a ToolException with message.
     * Uses {@link ToolErrorType#RETRYABLE} as default for parameter/format errors.
     *
     * @param message the error message
     */
    public ToolException(String message)
    {
        super(message);
        this.errorType = ToolErrorType.RETRYABLE;
        callMessage = null;
    }

    /**
     * Constructs a ToolException from a ToolCallMessage.
     * Uses {@link ToolErrorType#USER_VISIBLE} as default error type.
     *
     * @param callMessage the tool call message containing error information
     */
    public ToolException(ToolCallMessage callMessage)
    {
        super(callMessage.content);
        errorType = ToolErrorType.USER_VISIBLE;
        this.callMessage = callMessage;
    }

    /**
     * Returns the error type that indicates how LLM should handle this error.
     *
     * @return the error type
     */
    public ToolErrorType getErrorType()
    {
        return errorType;
    }

    /**
     * Returns the tool call message if this exception was constructed from one.
     *
     * @return the tool call message, or null if not available
     */
    public ToolCallMessage getCallMessage()
    {
        return callMessage;
    }
}
