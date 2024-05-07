/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import java.util.function.Predicate;

public interface ICodeCompletionTokenizer
{
    CodeCompletionToken getNext(int minLength, String text, Predicate<Character> isDelimiter);
}
