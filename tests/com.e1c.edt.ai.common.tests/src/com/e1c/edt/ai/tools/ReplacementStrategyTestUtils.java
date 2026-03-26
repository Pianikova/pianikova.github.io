package com.e1c.edt.ai.tools;

import java.util.ArrayList;
import java.util.List;

final class ReplacementStrategyTestUtils
{
    private ReplacementStrategyTestUtils()
    {
    }

    static List<String> toList(Iterable<String> iterable)
    {
        List<String> result = new ArrayList<>();
        for (String value : iterable)
        {
            result.add(value);
        }
        return result;
    }
}
