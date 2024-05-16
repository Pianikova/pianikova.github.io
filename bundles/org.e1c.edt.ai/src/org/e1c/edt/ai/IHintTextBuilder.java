/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

public interface IHintTextBuilder
{
    String build(String text, String prefix, int tabWidth, char lineFeedSing);
}
