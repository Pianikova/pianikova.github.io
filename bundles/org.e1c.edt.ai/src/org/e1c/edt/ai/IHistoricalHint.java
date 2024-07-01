/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

public interface IHistoricalHint
    extends IHint
{
    void initiaize(IHintHistory history, int maxLines, boolean isSingleWordMode);

    String pull(HintPart part);

    String pullChar(char ch);

    String rollback();
}
