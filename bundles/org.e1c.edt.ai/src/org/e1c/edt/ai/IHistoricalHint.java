/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

public interface IHistoricalHint
    extends IHint
{
    void initiaize(IHintHistory history, int maxLines, boolean isSingleWordMode);

    Text pull(HintPart part);

    Text pullChar(char ch);

    Text rollback();
}
