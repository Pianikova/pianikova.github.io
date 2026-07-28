/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Tests for JSON formatting helpers.
 */
@SuppressWarnings("nls")
public class JsonTest
{
    private final Json json = new Json();

    @Test
    public void compactsValidJson()
    {
        assertEquals("{\"value\":1,\"items\":[2]}",
            json.compactJson("{\n  \"value\": 1,\n  \"items\": [\n    2\n  ]\n}"));
    }

    @Test
    public void preservesInvalidJson()
    {
        assertEquals("plain\ntext", json.compactJson("plain\ntext"));
    }
}
