/**
 *
 */
package com.e1c.edt.ai;

public class LinkProvider
    implements ILinkProvider
{
    @Override
    public String getFileProtocol()
    {
        return EDT_FILE_PROTOCOL;
    }

    @Override
    public String getDiffProtocol()
    {
        return EDT_DIFF_PROTOCOL;
    }

    @SuppressWarnings("nls")
    @Override
    public String diff(String token)
    {
        return String.format("%s%s", EDT_DIFF_PROTOCOL, token == null ? "" : token);
    }

    @SuppressWarnings("nls")
    @Override
    public String file(String fullPath)
    {
        return String.format("%s%s", EDT_FILE_PROTOCOL, normalizedPath(fullPath));
    }

    @SuppressWarnings("nls")
    @Override
    public String file(String fullPath, int line, int column)
    {
        return String.format("%s%s%s%d%s%d", EDT_FILE_PROTOCOL, normalizedPath(fullPath), POSTFIX_SEPARATOR, line,
            POSITION_SEPARATOR, column);
    }

    @SuppressWarnings("nls")
    @Override
    public String file(String fullPath, int line, int column, int finishLine, int finishColumn)
    {
        return String.format("%s%s%s%d%s%d%s%d%s%d", EDT_FILE_PROTOCOL, normalizedPath(fullPath), POSTFIX_SEPARATOR,
            line, POSITION_SEPARATOR, column, POSITION_SEPARATOR, finishLine, POSITION_SEPARATOR, finishColumn);
    }

    private String normalizedPath(String path)
    {
        var normalizedPath = path.replace('\\', '/');

        // Escape colons in the path to avoid conflicts with position separator
        return normalizedPath.replace(COLON_CHAR, COLON_ESCAPE);
    }
}
