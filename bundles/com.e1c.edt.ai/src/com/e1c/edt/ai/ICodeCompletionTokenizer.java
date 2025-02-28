/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import java.util.function.Predicate;

public interface ICodeCompletionTokenizer
{
    CodeCompletionToken getNext(int minLength, String text, Predicate<Character> isDelimiter);
}
