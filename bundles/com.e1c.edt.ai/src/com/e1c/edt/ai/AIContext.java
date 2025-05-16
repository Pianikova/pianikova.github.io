/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import java.util.Objects;

import org.eclipse.jface.text.IDocument;

import com.e1c.edt.ai.assistent.model.ProjectId;
import com.google.common.base.Preconditions;

public class AIContext
{
    private final ProjectId projectId;
    private final int editorOffset;
    private final String source;
    private final int sourceOffset;
    private final String path;
    private final int textOffset;
    private final String text;
    private final String prefix;
    private final String sufix;
    private final int start;
    private final int finish;
    private final IDocument document;

    public AIContext(ProjectId projectId, int caretOffset, String source, int sourceOffset,
        String path, String text,
        int textOffset,
        String prefix,
        String sufix, int start, int finish, IDocument document)
    {
        Preconditions.checkNotNull(projectId);
        Preconditions.checkNotNull(source);
        Preconditions.checkArgument(sourceOffset >= 0);
        Preconditions.checkNotNull(path);
        Preconditions.checkNotNull(text);
        Preconditions.checkArgument(textOffset >= 0);
        Preconditions.checkNotNull(prefix);
        Preconditions.checkNotNull(sufix);
        this.projectId = projectId;
        this.editorOffset = caretOffset;
        this.source = source;
        this.sourceOffset = sourceOffset;
        this.path = path;
        this.textOffset = textOffset;
        this.text = text;
        this.prefix = prefix;
        this.sufix = sufix;
        this.start = start;
        this.finish = finish;
        this.document = document;
    }

    public AIContext(ProjectId projectId, int caretOffset, String source, int sourceOffset,
        String path, String text,
        int textOffset, IDocument document)
    {
        this(projectId, caretOffset, source, sourceOffset, path, text, textOffset, "", "", 0, 0, document); //$NON-NLS-1$//$NON-NLS-2$
    }

    public ProjectId getProjectId()
    {
        return projectId;
    }

    public int getCaretOffset()
    {
        return editorOffset;
    }

    public String getSource()
    {
        return source;
    }

    public int getSourceOffset()
    {
        return sourceOffset;
    }

    public String getPath()
    {
        return path;
    }

    public String getText()
    {
        return text;
    }

    public int getTextOffset()
    {
        return textOffset;
    }

    public String getPrefix()
    {
        return prefix;
    }

    public String getSufix()
    {
        return sufix;
    }

    public int getStart()
    {
        return start;
    }

    public int getFinish()
    {
        return finish;
    }

    public IDocument getDocument()
    {
        return document;
    }

    @Override
    public String toString()
    {
        var str = new StringBuilder();

        str.append("project:"); //$NON-NLS-1$
        str.append(projectId);
        str.append(System.lineSeparator());

        str.append("path:"); //$NON-NLS-1$
        str.append(path);
        str.append(System.lineSeparator());

        str.append("cursorOffset:"); //$NON-NLS-1$
        str.append(textOffset);
        str.append(System.lineSeparator());

        str.append("start:"); //$NON-NLS-1$
        str.append(getStart());
        str.append(System.lineSeparator());

        str.append("finish:"); //$NON-NLS-1$
        str.append(getFinish());
        str.append(System.lineSeparator());

        var textWithCursor = text.substring(0, textOffset) + "█" + text.substring(textOffset); //$NON-NLS-1$

        str.append("text:"); //$NON-NLS-1$
        str.append(format(textWithCursor));
        str.append(System.lineSeparator());

        str.append("prefix:"); //$NON-NLS-1$
        str.append(format(prefix));
        str.append(System.lineSeparator());

        str.append("sufix:"); //$NON-NLS-1$
        str.append(format(sufix));
        str.append(System.lineSeparator());

        str.append("raw text:"); //$NON-NLS-1$
        str.append(System.lineSeparator());
        str.append(textWithCursor);

        return str.toString();
    }

    @SuppressWarnings("nls")
    private static String format(String text)
    {
        return "[" + text.replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "]";
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(source, sourceOffset);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AIContext other = (AIContext)obj;
        return Objects.equals(source, other.source) && sourceOffset == other.sourceOffset;
    }
}