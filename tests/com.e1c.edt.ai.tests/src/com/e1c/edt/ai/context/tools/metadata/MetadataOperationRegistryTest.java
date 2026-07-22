/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context.tools.metadata;

import org.junit.Assert;
import org.junit.Test;

import com.google.gson.JsonParser;

public class MetadataOperationRegistryTest
{
    private final MetadataOperationRegistry registry = new MetadataOperationRegistry();

    @Test
    public void shouldAcceptExactOperationContract()
    {
        var arguments = JsonParser.parseString("{\"operation\":\"addObjectAttribute\","
            + "\"project_name\":\"Demo\",\"object_name\":\"Catalog.Products\","
            + "\"name\":\"Article\",\"type\":\"String\",\"length\":20}").getAsJsonObject();

        Assert.assertTrue(registry.validate(arguments).toString(), registry.validate(arguments).isEmpty());
    }

    @Test
    public void shouldRejectUnknownOperation()
    {
        var arguments = JsonParser.parseString("{\"operation\":\"executeJava\"}").getAsJsonObject();

        var errors = registry.validate(arguments);

        Assert.assertEquals(1, errors.size());
        Assert.assertTrue(errors.get(0).contains("Unknown operation")); //$NON-NLS-1$
    }

    @Test
    public void shouldRejectMissingAndInventedParameters()
    {
        var arguments = JsonParser.parseString("{\"operation\":\"createObject\","
            + "\"project_name\":\"Demo\",\"java_code\":\"anything\"}").getAsJsonObject();

        var errors = registry.validate(arguments);

        Assert.assertEquals(2, errors.size());
        Assert.assertTrue(errors.stream().anyMatch(error -> error.contains("object_name"))); //$NON-NLS-1$
        Assert.assertTrue(errors.stream().anyMatch(error -> error.contains("java_code"))); //$NON-NLS-1$
    }

    @Test
    public void shouldExposeExactOperationNames()
    {
        Assert.assertTrue(registry.names().contains("addTabularSectionAttribute")); //$NON-NLS-1$
        Assert.assertFalse(registry.names().contains("createAttribute")); //$NON-NLS-1$
        Assert.assertFalse(registry.names().contains("setObjectComment")); //$NON-NLS-1$
    }
}
