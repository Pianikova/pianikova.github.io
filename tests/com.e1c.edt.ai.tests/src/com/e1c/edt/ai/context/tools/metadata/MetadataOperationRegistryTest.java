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
        var arguments = JsonParser.parseString("{\"operation\":\"addObjectAttribute\"," //$NON-NLS-1$
            + "\"project_name\":\"Demo\",\"object_name\":\"Catalog.Products\"," //$NON-NLS-1$
            + "\"name\":\"Article\",\"type\":\"String\",\"length\":20}").getAsJsonObject(); //$NON-NLS-1$

        Assert.assertTrue(registry.validate(arguments).toString(), registry.validate(arguments).isEmpty());
    }

    @Test
    public void shouldRejectUnknownOperation()
    {
        var arguments = JsonParser.parseString("{\"operation\":\"executeJava\"}").getAsJsonObject(); //$NON-NLS-1$

        var errors = registry.validate(arguments);

        Assert.assertEquals(1, errors.size());
        Assert.assertTrue(errors.get(0).contains("Unknown operation")); //$NON-NLS-1$
    }

    @Test
    public void shouldRejectMissingAndInventedParameters()
    {
        var arguments = JsonParser.parseString("{\"operation\":\"createObject\"," //$NON-NLS-1$
            + "\"project_name\":\"Demo\",\"java_code\":\"anything\"}").getAsJsonObject(); //$NON-NLS-1$

        var errors = registry.validate(arguments);

        Assert.assertEquals(2, errors.size());
        Assert.assertTrue(errors.stream().anyMatch(error -> error.contains("object_name"))); //$NON-NLS-1$
        Assert.assertTrue(errors.stream().anyMatch(error -> error.contains("java_code"))); //$NON-NLS-1$
    }

    @Test
    public void shouldExposeExactOperationNames()
    {
        Assert.assertTrue(registry.names().contains("addTabularSectionAttribute")); //$NON-NLS-1$
        Assert.assertTrue(registry.names().contains("inspectObject")); //$NON-NLS-1$
        Assert.assertTrue(registry.names().contains("setChildType")); //$NON-NLS-1$
        Assert.assertTrue(registry.names().contains("addDocumentRegister")); //$NON-NLS-1$
        Assert.assertTrue(registry.names().contains("createObjectForm")); //$NON-NLS-1$
        Assert.assertTrue(registry.names().contains("createObjectTemplate")); //$NON-NLS-1$
        Assert.assertFalse(registry.names().contains("createAttribute")); //$NON-NLS-1$
        Assert.assertFalse(registry.names().contains("setObjectComment")); //$NON-NLS-1$
    }

    @Test
    public void shouldAcceptInspectAndChildMutationContracts()
    {
        var inspect = JsonParser.parseString("{\"operation\":\"inspectObject\"," //$NON-NLS-1$
            + "\"project_name\":\"Demo\",\"object_name\":\"Document.Order\"}").getAsJsonObject(); //$NON-NLS-1$
        var child = JsonParser.parseString("{\"operation\":\"setChildType\"," //$NON-NLS-1$
            + "\"project_name\":\"Demo\",\"object_name\":\"Document.Order.Lines\"," //$NON-NLS-1$
            + "\"child_kind\":\"tabular_section_attribute\",\"name\":\"Quantity\"," //$NON-NLS-1$
            + "\"type\":\"Number\",\"length\":12,\"precision\":3}").getAsJsonObject(); //$NON-NLS-1$

        Assert.assertTrue(registry.validate(inspect).toString(), registry.validate(inspect).isEmpty());
        Assert.assertTrue(registry.validate(child).toString(), registry.validate(child).isEmpty());
    }

    @Test
    public void shouldRequireGeneratedArtifactKinds()
    {
        var form = JsonParser.parseString("{\"operation\":\"createObjectForm\"," //$NON-NLS-1$
            + "\"project_name\":\"Demo\",\"object_name\":\"Catalog.Products\",\"name\":\"ItemForm\"}") //$NON-NLS-1$
                .getAsJsonObject();
        var template = JsonParser.parseString("{\"operation\":\"createObjectTemplate\"," //$NON-NLS-1$
            + "\"project_name\":\"Demo\",\"object_name\":\"Report.Sales\",\"name\":\"Main\"}") //$NON-NLS-1$
                .getAsJsonObject();

        Assert.assertTrue(registry.validate(form).stream().anyMatch(error -> error.contains("form_type"))); //$NON-NLS-1$
        Assert.assertTrue(registry.validate(template).stream().anyMatch(error -> error.contains("template_type"))); //$NON-NLS-1$
    }
}
