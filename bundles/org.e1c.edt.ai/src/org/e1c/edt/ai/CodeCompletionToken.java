/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import java.util.Objects;

import com.google.common.base.Preconditions;

public class CodeCompletionToken
{
    private String value;
    private String text;

    public CodeCompletionToken(String value, String text)
    {
        Preconditions.checkNotNull(value);
        Preconditions.checkNotNull(text);
        this.value = value;
        this.text = text;
    }

    public String getValue()
    {
        return value;
    }

    public String getText()
    {
        return text;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(text, value);
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
        CodeCompletionToken other = (CodeCompletionToken)obj;
        return Objects.equals(text, other.text) && Objects.equals(value, other.value);
    }

    @SuppressWarnings("nls")
    @Override
    public String toString()
    {
        return "CodeCompletionToken [value=" + value + ", text=" + text + "]";
    }
}
