/**
 * Provides functionality for creating links to files and diffs in the EDT environment.
 */
package com.e1c.edt.ai;

public interface ILinkProvider
{
    final String EDT_FILE_PROTOCOL = "edt-file://"; //$NON-NLS-1$
    final String EDT_DIFF_PROTOCOL = "edt-diff://"; //$NON-NLS-1$
    final String POSTFIX_SEPARATOR = ":"; //$NON-NLS-1$
    final String POSITION_SEPARATOR = ":"; //$NON-NLS-1$
    final String COLON_ESCAPE = "%3A"; //$NON-NLS-1$
    final String COLON_CHAR = ":"; //$NON-NLS-1$

    /**
     * Returns the file protocol used for creating file links.
     *
     * @return the file protocol string (e.g., "edt-file://")
     */
    String getFileProtocol();

    /**
     * Returns the diff protocol used for creating diff links.
     *
     * @return the diff protocol string (e.g., "edt-diff://")
     */
    String getDiffProtocol();

    /**
     * Creates a link to a file.
     *
     * @param fullPath the absolute path to the file
     * @return a link in the format {@code <protocol><fullPath>}
     */
    String file(String fullPath);

    /**
     * Creates a link to a specific position in a file.
     *
     * @param fullPath the absolute path to the file
     * @param line the line number (0-based)
     * @param column the column number (0-based)
     * @return a link in the format {@code <protocol><fullPath>:<line>:<column>}
     */
    String file(String fullPath, int line, int column);

    /**
     * Creates a link to a specific range in a file.
     *
     * @param fullPath the absolute path to the file
     * @param line the start line number (0-based)
     * @param column the start column number (0-based)
     * @param finishLine the end line number (0-based)
     * @param finishColumn the end column number (0-based)
     * @return a link in the format {@code <protocol><fullPath>:<line>:<column>:<finishLine>:<finishColumn>}
     */
    String file(String fullPath, int line, int column, int finishLine, int finishColumn);

    /**
     * Builds a diff-preview link carrying an opaque token that the IDE side resolves to a stored
     * diff payload.
     *
     * @param token the diff-preview token (e.g. the tool-call id)
     * @return a {@code edt-diff://<token>} link
     */
    String diff(String token);
}
