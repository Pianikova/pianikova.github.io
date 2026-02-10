/**
 *
 */
package com.e1c.edt.ai;

public interface ILinkProvider
{
    final String EDT_FILE_PROTOCOL = "edt-file://"; //$NON-NLS-1$
    final String POSTFIX_SEPARATOR = ":"; //$NON-NLS-1$
    final String POSITION_SEPARATOR = ":"; //$NON-NLS-1$
    final String COLON_ESCAPE = "%3A"; //$NON-NLS-1$
    final String COLON_CHAR = ":"; //$NON-NLS-1$

    String getFileProtocol();

    String file(String fullPath);

    String file(String fullPath, int line, int column);

    String file(String fullPath, int line, int column, int finishLine, int finishColumn);
}
