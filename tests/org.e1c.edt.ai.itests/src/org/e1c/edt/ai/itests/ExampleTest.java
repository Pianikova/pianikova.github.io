/**
 * Copyright (C) 2023, 1C-Soft LLC
 */
package org.e1c.edt.ai.itests;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import com._1c.g5.v8.dt.testing.TestingWorkspace;

/**
 * Пример теста
 *
 * Больше информации о написании тестов см https://edt.1c.ru/dev/ru/docs/plugins/dev/testing/
 */
public class ExampleTest
{
    @Rule
    public TestingWorkspace testingWorkspace = new TestingWorkspace();

    @Test
    public void testMethodStartsWithCapitalLetterWarning() throws Exception
    {
        Assert.assertTrue(true);
    }

}
