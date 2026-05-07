/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.assistent.model;

import java.util.Collections;
import java.util.Map;

/**
 * @author Bogdan Sushkov
 *
 */
public class SkillMetadata
{
    private final Map<String, String> values;

    public SkillMetadata(Map<String, String> values)
    {
        this.values = values == null ? Collections.emptyMap() : Collections.unmodifiableMap(values);
    }

    public String get(String key)
    {
        return values.get(key);
    }

    public Map<String, String> getValues()
    {
        return values;
    }
}
