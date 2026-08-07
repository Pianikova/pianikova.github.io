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

        Assert.assertEquals(3, errors.size());
        Assert.assertTrue(errors.stream().anyMatch(error -> error.contains("object_name"))); //$NON-NLS-1$
        Assert.assertTrue(errors.stream().anyMatch(error -> error.contains("java_code"))); //$NON-NLS-1$
        Assert.assertTrue(errors.stream().anyMatch(error -> error.contains("Correct example"))); //$NON-NLS-1$
    }

    @Test
    public void shouldExposeExactOperationNames()
    {
        Assert.assertTrue(registry.names().contains("addTabularSectionAttribute")); //$NON-NLS-1$
        Assert.assertTrue(registry.names().contains("inspectObject")); //$NON-NLS-1$
        Assert.assertTrue(registry.names().contains("setChildType")); //$NON-NLS-1$
        Assert.assertTrue(registry.names().contains("addDocumentRegister")); //$NON-NLS-1$
        Assert.assertTrue(registry.names().contains("createObjectForm")); //$NON-NLS-1$
        Assert.assertTrue(registry.names().contains("setScheduledJobSchedule")); //$NON-NLS-1$
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
    public void shouldExposeFormContentOperations()
    {
        Assert.assertTrue(registry.names().contains("inspectForm")); //$NON-NLS-1$
        Assert.assertTrue(registry.names().contains("addFormAttribute")); //$NON-NLS-1$
        Assert.assertTrue(registry.names().contains("addFormField")); //$NON-NLS-1$
        Assert.assertTrue(registry.names().contains("addFormGroup")); //$NON-NLS-1$
        Assert.assertTrue(registry.names().contains("addFormCommand")); //$NON-NLS-1$
        Assert.assertTrue(registry.names().contains("addFormButton")); //$NON-NLS-1$
        Assert.assertTrue(registry.names().contains("moveFormItem")); //$NON-NLS-1$
        Assert.assertTrue(registry.names().contains("setFormItemProperty")); //$NON-NLS-1$
    }

    @Test
    public void shouldAcceptFormContentContracts()
    {
        var attribute = JsonParser.parseString("{\"operation\":\"addFormAttribute\",\"project_name\":\"Demo\"," //$NON-NLS-1$
            + "\"object_name\":\"Catalog.Products.Form.ItemForm\",\"name\":\"Comment\",\"type\":\"String\"," //$NON-NLS-1$
            + "\"length\":200,\"main\":false,\"title\":\"Comment\",\"language_code\":\"ru\"}").getAsJsonObject(); //$NON-NLS-1$
        var field = JsonParser.parseString("{\"operation\":\"addFormField\",\"project_name\":\"Demo\"," //$NON-NLS-1$
            + "\"object_name\":\"Catalog.Products.Form.ItemForm\",\"name\":\"Comment\"," //$NON-NLS-1$
            + "\"data_path\":\"Object.Comment\",\"parent\":\"MainGroup\",\"position\":0," //$NON-NLS-1$
            + "\"item_type\":\"InputField\"}").getAsJsonObject(); //$NON-NLS-1$
        var command = JsonParser.parseString("{\"operation\":\"addFormCommand\",\"project_name\":\"Demo\"," //$NON-NLS-1$
            + "\"object_name\":\"Catalog.Products.Form.ItemForm\",\"name\":\"Fill\",\"handler\":\"Fill\"}") //$NON-NLS-1$
                .getAsJsonObject();

        Assert.assertTrue(registry.validate(attribute).toString(), registry.validate(attribute).isEmpty());
        Assert.assertTrue(registry.validate(field).toString(), registry.validate(field).isEmpty());
        Assert.assertTrue(registry.validate(command).toString(), registry.validate(command).isEmpty());
    }

    @Test
    public void shouldRequireFormContentKeyParameters()
    {
        var field = JsonParser.parseString("{\"operation\":\"addFormField\",\"project_name\":\"Demo\"," //$NON-NLS-1$
            + "\"object_name\":\"Catalog.Products.Form.ItemForm\",\"name\":\"Comment\"}").getAsJsonObject(); //$NON-NLS-1$
        var group = JsonParser.parseString("{\"operation\":\"addFormGroup\",\"project_name\":\"Demo\"," //$NON-NLS-1$
            + "\"object_name\":\"Catalog.Products.Form.ItemForm\",\"name\":\"Pages\"}").getAsJsonObject(); //$NON-NLS-1$
        var button = JsonParser.parseString("{\"operation\":\"addFormButton\",\"project_name\":\"Demo\"," //$NON-NLS-1$
            + "\"object_name\":\"Catalog.Products.Form.ItemForm\",\"name\":\"FillButton\"}").getAsJsonObject(); //$NON-NLS-1$

        Assert.assertTrue(registry.validate(field).stream().anyMatch(error -> error.contains("data_path"))); //$NON-NLS-1$
        Assert.assertTrue(registry.validate(group).stream().anyMatch(error -> error.contains("group_type"))); //$NON-NLS-1$
        Assert.assertTrue(registry.validate(button).stream().anyMatch(error -> error.contains("command_name"))); //$NON-NLS-1$
    }

    @Test
    public void shouldAcceptLanguageCodeOnMultilingualProperties()
    {
        var object = JsonParser.parseString("{\"operation\":\"setObjectProperty\",\"project_name\":\"Demo\"," //$NON-NLS-1$
            + "\"object_name\":\"InformationRegister.Prices\",\"property_name\":\"listPresentation\"," //$NON-NLS-1$
            + "\"property_value\":\"Prices\",\"language_code\":\"ru\"}").getAsJsonObject(); //$NON-NLS-1$
        var child = JsonParser.parseString("{\"operation\":\"setChildProperty\",\"project_name\":\"Demo\"," //$NON-NLS-1$
            + "\"object_name\":\"Catalog.Products\",\"child_kind\":\"object_attribute\",\"name\":\"Article\"," //$NON-NLS-1$
            + "\"property_name\":\"synonym\",\"property_value\":\"Артикул\",\"language_code\":\"ru\"}") //$NON-NLS-1$
                .getAsJsonObject();

        Assert.assertTrue(registry.validate(object).toString(), registry.validate(object).isEmpty());
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

    @Test
    public void shouldAcceptRedundantNameForCreateObject()
    {
        var arguments = JsonParser.parseString("{\"operation\":\"createObject\"," //$NON-NLS-1$
            + "\"project_name\":\"Demo\",\"object_name\":\"Enum.Statuses\",\"name\":\"Statuses\"}") //$NON-NLS-1$
            .getAsJsonObject();

        Assert.assertTrue(registry.validate(arguments).toString(), registry.validate(arguments).isEmpty());
    }

    @Test
    public void shouldNormalizeCommonAddFormFieldArguments()
    {
        var arguments = JsonParser.parseString("{\"operation\":\"addFormField\"," //$NON-NLS-1$
            + "\"project_name\":\"Demo\",\"object_name\":\"DataProcessor.Test.Form.Form\"," //$NON-NLS-1$
            + "\"dataPath\":\"Object.Comment\"}").getAsJsonObject(); //$NON-NLS-1$

        EditMetadataMcpTool.normalizeCommonModelArguments(arguments);

        Assert.assertEquals("Object.Comment", arguments.get("data_path").getAsString()); //$NON-NLS-1$ //$NON-NLS-2$
        Assert.assertEquals("Comment", arguments.get("name").getAsString()); //$NON-NLS-1$ //$NON-NLS-2$
        Assert.assertFalse(arguments.has("dataPath")); //$NON-NLS-1$
        Assert.assertTrue(registry.validate(arguments).toString(), registry.validate(arguments).isEmpty());
    }

    @Test
    public void shouldRouteFormInspectionToInspectForm()
    {
        var arguments = JsonParser.parseString("{\"operation\":\"inspectObject\"," //$NON-NLS-1$
            + "\"project_name\":\"Demo\"," //$NON-NLS-1$
            + "\"object_name\":\"Catalog.Products.Form.ItemForm\"}").getAsJsonObject(); //$NON-NLS-1$

        EditMetadataMcpTool.normalizeCommonModelArguments(arguments);

        Assert.assertEquals("inspectForm", arguments.get("operation").getAsString()); //$NON-NLS-1$ //$NON-NLS-2$
        Assert.assertTrue(registry.validate(arguments).toString(), registry.validate(arguments).isEmpty());
    }

    @Test
    public void shouldNormalizeCommonPropertyAndFormCommandArguments()
    {
        var property = JsonParser.parseString("{\"operation\":\"setObjectProperty\"," //$NON-NLS-1$
            + "\"project_name\":\"Demo\",\"object_name\":\"Catalog.Products\",\"name\":\"Products\"," //$NON-NLS-1$
            + "\"title\":\"Products\"," //$NON-NLS-1$
            + "\"property_name\":\"comment\",\"property_value\":\"Managed\"}").getAsJsonObject(); //$NON-NLS-1$
        var command = JsonParser.parseString("{\"operation\":\"addFormCommand\"," //$NON-NLS-1$
            + "\"project_name\":\"Demo\",\"object_name\":\"Catalog.Products.Form.ItemForm\"," //$NON-NLS-1$
            + "\"command_name\":\"Save\",\"handler\":\"Save\",\"form_type\":\"GENERIC\"}") //$NON-NLS-1$
            .getAsJsonObject();

        EditMetadataMcpTool.normalizeCommonModelArguments(property);
        EditMetadataMcpTool.normalizeCommonModelArguments(command);

        Assert.assertFalse(property.has("name")); //$NON-NLS-1$
        Assert.assertFalse(property.has("title")); //$NON-NLS-1$
        Assert.assertEquals("Save", command.get("name").getAsString()); //$NON-NLS-1$ //$NON-NLS-2$
        Assert.assertFalse(command.has("command_name")); //$NON-NLS-1$
        Assert.assertFalse(command.has("form_type")); //$NON-NLS-1$
        Assert.assertTrue(registry.validate(property).toString(), registry.validate(property).isEmpty());
        Assert.assertTrue(registry.validate(command).toString(), registry.validate(command).isEmpty());
    }

    @Test
    public void shouldRouteChildTypePropertyToSetChildType()
    {
        var arguments = JsonParser.parseString("{\"operation\":\"setChildProperty\"," //$NON-NLS-1$
            + "\"project_name\":\"Demo\",\"object_name\":\"InformationRegister.Prices\"," //$NON-NLS-1$
            + "\"child_kind\":\"register_resource\",\"name\":\"Price\"," //$NON-NLS-1$
            + "\"property_name\":\"type\",\"type\":\"String\",\"length\":200,\"title\":\"Price\"}") //$NON-NLS-1$
            .getAsJsonObject();

        EditMetadataMcpTool.normalizeCommonModelArguments(arguments);

        Assert.assertEquals("setChildType", arguments.get("operation").getAsString()); //$NON-NLS-1$ //$NON-NLS-2$
        Assert.assertFalse(arguments.has("property_name")); //$NON-NLS-1$
        Assert.assertFalse(arguments.has("title")); //$NON-NLS-1$
        Assert.assertTrue(registry.validate(arguments).toString(), registry.validate(arguments).isEmpty());
    }

    @Test
    public void shouldNormalizeHelpAndCommonFormAliases()
    {
        var help = JsonParser.parseString("{\"operation\":\"help\",\"topic\":\"createObject\"," //$NON-NLS-1$
            + "\"project_name\":\"Demo\",\"object_name\":\"Catalog\"}").getAsJsonObject(); //$NON-NLS-1$
        var inspect = JsonParser.parseString("{\"operation\":\"inspectForm\",\"project_name\":\"Demo\"," //$NON-NLS-1$
            + "\"object_name\":\"Catalog.Products\"}").getAsJsonObject(); //$NON-NLS-1$
        var remove = JsonParser.parseString("{\"operation\":\"removeFormAttribute\"," //$NON-NLS-1$
            + "\"project_name\":\"Demo\",\"object_name\":\"Catalog.Products.Form.ItemForm\"," //$NON-NLS-1$
            + "\"property_name\":\"Comment\"}").getAsJsonObject(); //$NON-NLS-1$
        var button = JsonParser.parseString("{\"operation\":\"addFormButton\"," //$NON-NLS-1$
            + "\"project_name\":\"Demo\",\"object_name\":\"Catalog.Products.Form.ItemForm\"," //$NON-NLS-1$
            + "\"command_name\":\"Save\"}").getAsJsonObject(); //$NON-NLS-1$

        EditMetadataMcpTool.normalizeCommonModelArguments(help);
        EditMetadataMcpTool.normalizeCommonModelArguments(inspect);
        EditMetadataMcpTool.normalizeCommonModelArguments(remove);
        EditMetadataMcpTool.normalizeCommonModelArguments(button);

        Assert.assertFalse(help.has("project_name")); //$NON-NLS-1$
        Assert.assertFalse(help.has("object_name")); //$NON-NLS-1$
        Assert.assertEquals("inspectObject", inspect.get("operation").getAsString()); //$NON-NLS-1$ //$NON-NLS-2$
        Assert.assertEquals("Comment", remove.get("name").getAsString()); //$NON-NLS-1$ //$NON-NLS-2$
        Assert.assertFalse(remove.has("property_name")); //$NON-NLS-1$
        Assert.assertEquals("Save", button.get("name").getAsString()); //$NON-NLS-1$ //$NON-NLS-2$
        Assert.assertTrue(registry.validate(help).toString(), registry.validate(help).isEmpty());
        Assert.assertTrue(registry.validate(inspect).toString(), registry.validate(inspect).isEmpty());
        Assert.assertTrue(registry.validate(remove).toString(), registry.validate(remove).isEmpty());
        Assert.assertTrue(registry.validate(button).toString(), registry.validate(button).isEmpty());
    }

    @Test
    public void shouldReturnOperationExampleAfterContractError()
    {
        var arguments = JsonParser.parseString("{\"operation\":\"addFormCommand\"," //$NON-NLS-1$
            + "\"project_name\":\"Demo\",\"object_name\":\"Catalog.Products.Form.ItemForm\"," //$NON-NLS-1$
            + "\"parent\":\"Commands\"}").getAsJsonObject(); //$NON-NLS-1$

        var errors = registry.validate(arguments);

        Assert.assertTrue(errors.toString(), errors.stream().anyMatch(error -> error.contains("Correct example") //$NON-NLS-1$
            && error.contains("addFormCommand") && error.contains("\"name\""))); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    public void shouldNormalizeCreateObjectLanguageAndObjectPropertyCase()
    {
        var create = JsonParser.parseString("{\"operation\":\"createObject\",\"language_code\":\"ru\"}") //$NON-NLS-1$
            .getAsJsonObject();
        var property = JsonParser.parseString("{\"operation\":\"setObjectProperty\"," //$NON-NLS-1$
            + "\"property_name\":\"Server\"}").getAsJsonObject(); //$NON-NLS-1$

        EditMetadataMcpTool.normalizeCommonModelArguments(create);
        EditMetadataMcpTool.normalizeCommonModelArguments(property);

        Assert.assertFalse(create.has("language_code")); //$NON-NLS-1$
        Assert.assertEquals("server", property.get("property_name").getAsString()); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
