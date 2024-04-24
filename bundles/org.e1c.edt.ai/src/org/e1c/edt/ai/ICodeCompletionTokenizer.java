/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

/**
 * @author Nikolay Pyanikov
 *
 */
public interface ICodeCompletionTokenizer
{
    CodeCompletionToken getNext(String text);
}
