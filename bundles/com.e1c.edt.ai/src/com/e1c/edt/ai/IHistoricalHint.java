/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

public interface IHistoricalHint
    extends IHint
{
    void initiaize(IHintHistory history, int maxLines, boolean isSingleWordMode);

    Text pull(HintPart part);

    Text pullChar(char ch);

    Text rollback();
}
