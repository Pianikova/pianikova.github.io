/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context.tools.metadata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonObject;
import com.google.inject.Singleton;

@Singleton
public final class MetadataOperationRegistry
{
    private static final Set<String> TYPE_PARAMETERS = set("length", "precision", "date_fractions"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

    private final Map<String, MetadataOperationDescriptor> operations = new LinkedHashMap<>();

    public MetadataOperationRegistry()
    {
        add("help", "Lists operations or returns exact help for one operation.", set(), set("topic"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "{\"operation\":\"help\",\"topic\":\"createObject\"}"); //$NON-NLS-1$
        add("inspectObject", "Reads an object, its scalar properties, children, types, forms, templates, and register links.", //$NON-NLS-1$ //$NON-NLS-2$
            set("project_name", "object_name"), set(), //$NON-NLS-1$ //$NON-NLS-2$
            "{\"operation\":\"inspectObject\",\"project_name\":\"MyProject\",\"object_name\":\"Document.Order\"}"); //$NON-NLS-1$
        add("createObject", "Creates any top-level object type listed by help topic=objectTypes.", //$NON-NLS-1$ //$NON-NLS-2$
            set("project_name", "object_name"), //$NON-NLS-1$ //$NON-NLS-2$
            set("title", "dry_run"), //$NON-NLS-1$ //$NON-NLS-2$
            "{\"operation\":\"createObject\",\"project_name\":\"MyProject\",\"object_name\":\"Catalog.Products\",\"title\":\"Products\"}"); //$NON-NLS-1$
        add("setObjectProperty", //$NON-NLS-1$
            "Changes one scalar property of an existing top-level object. Use object_name=Configuration to edit" //$NON-NLS-1$
                + " configuration properties such as version, compatibilityMode, scriptVariant, vendor," //$NON-NLS-1$
                + " defaultRunMode, or namePrefix.", //$NON-NLS-1$
            set("project_name", "object_name", "property_name", "property_value"), set("dry_run"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            "{\"operation\":\"setObjectProperty\",\"project_name\":\"MyProject\",\"object_name\":\"Catalog.Products\",\"property_name\":\"comment\",\"property_value\":\"Managed by AI\"}"); //$NON-NLS-1$
        add("renameObject", "Renames a top-level object using the EDT refactoring service.", //$NON-NLS-1$ //$NON-NLS-2$
            set("project_name", "object_name", "new_name"), set("dry_run"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            "{\"operation\":\"renameObject\",\"project_name\":\"MyProject\",\"object_name\":\"Catalog.OldName\",\"new_name\":\"NewName\"}"); //$NON-NLS-1$
        add("removeObject", "Deletes a top-level object using the EDT refactoring service.", //$NON-NLS-1$ //$NON-NLS-2$
            set("project_name", "object_name"), set("dry_run"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "{\"operation\":\"removeObject\",\"project_name\":\"MyProject\",\"object_name\":\"Catalog.Temp\"}"); //$NON-NLS-1$

        addTypedChild("addObjectAttribute", //$NON-NLS-1$
            "Adds an attribute directly to a top-level object; never use it for a tabular-section attribute."); //$NON-NLS-1$
        addRemoveChild("removeObjectAttribute", "Removes an attribute from an object."); //$NON-NLS-1$ //$NON-NLS-2$
        add("addTabularSection", "Adds a tabular section to an object.", set("project_name", "object_name", "name"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            set("title", "dry_run"), //$NON-NLS-1$ //$NON-NLS-2$
            "{\"operation\":\"addTabularSection\",\"project_name\":\"MyProject\",\"object_name\":\"Document.Order\",\"name\":\"Lines\"}"); //$NON-NLS-1$
        addRemoveChild("removeTabularSection", "Removes a tabular section from an object."); //$NON-NLS-1$ //$NON-NLS-2$
        addTypedChild("addTabularSectionAttribute", //$NON-NLS-1$
            "Adds an attribute inside an existing tabular section; object_name must be Type.Object.TabularSection."); //$NON-NLS-1$
        addRemoveChild("removeTabularSectionAttribute", "Removes an attribute from a tabular section."); //$NON-NLS-1$ //$NON-NLS-2$
        add("addEnumValue", "Adds a value to an enum.", set("project_name", "object_name", "name"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            set("title", "dry_run"), //$NON-NLS-1$ //$NON-NLS-2$
            "{\"operation\":\"addEnumValue\",\"project_name\":\"MyProject\",\"object_name\":\"Enum.Status\",\"name\":\"New\"}"); //$NON-NLS-1$
        addRemoveChild("removeEnumValue", "Removes a value from an enum."); //$NON-NLS-1$ //$NON-NLS-2$
        add("addRegisterField", "Adds a dimension, resource, or attribute to a register.", //$NON-NLS-1$ //$NON-NLS-2$
            set("project_name", "object_name", "name", "field_kind", "type"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            union(TYPE_PARAMETERS, set("title", "dry_run")), //$NON-NLS-1$ //$NON-NLS-2$
            "{\"operation\":\"addRegisterField\",\"project_name\":\"MyProject\",\"object_name\":\"InformationRegister.Prices\",\"name\":\"Price\",\"field_kind\":\"resource\",\"type\":\"Number\",\"length\":15,\"precision\":2}"); //$NON-NLS-1$
        add("removeRegisterField", "Removes a dimension, resource, or attribute from a register.", //$NON-NLS-1$ //$NON-NLS-2$
            set("project_name", "object_name", "name", "field_kind"), set("dry_run"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            "{\"operation\":\"removeRegisterField\",\"project_name\":\"MyProject\",\"object_name\":\"InformationRegister.Prices\",\"name\":\"Price\",\"field_kind\":\"resource\"}"); //$NON-NLS-1$

        add("setChildProperty", "Changes a scalar property, synonym, or comment of an existing child.", //$NON-NLS-1$ //$NON-NLS-2$
            set("project_name", "object_name", "child_kind", "name", "property_name", "property_value"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
            set("dry_run"), //$NON-NLS-1$
            "{\"operation\":\"setChildProperty\",\"project_name\":\"MyProject\",\"object_name\":\"Document.Order.Lines\",\"child_kind\":\"tabular_section_attribute\",\"name\":\"Quantity\",\"property_name\":\"comment\",\"property_value\":\"Ordered quantity\"}"); //$NON-NLS-1$
        add("setChildType", "Changes the type of an existing attribute or register field.", //$NON-NLS-1$ //$NON-NLS-2$
            set("project_name", "object_name", "child_kind", "name", "type"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            union(TYPE_PARAMETERS, set("dry_run")), //$NON-NLS-1$
            "{\"operation\":\"setChildType\",\"project_name\":\"MyProject\",\"object_name\":\"Document.Order.Lines\",\"child_kind\":\"tabular_section_attribute\",\"name\":\"Quantity\",\"type\":\"Number\",\"length\":15,\"precision\":3}"); //$NON-NLS-1$
        add("renameChild", "Renames an existing child metadata element.", //$NON-NLS-1$ //$NON-NLS-2$
            set("project_name", "object_name", "child_kind", "name", "new_name"), set("dry_run"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
            "{\"operation\":\"renameChild\",\"project_name\":\"MyProject\",\"object_name\":\"Document.Order\",\"child_kind\":\"tabular_section\",\"name\":\"Lines\",\"new_name\":\"Goods\"}"); //$NON-NLS-1$
        add("addDocumentRegister", "Adds an accumulation, accounting, or calculation register to a document's register records.", //$NON-NLS-1$ //$NON-NLS-2$
            set("project_name", "object_name", "related_object_name"), set("dry_run"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            "{\"operation\":\"addDocumentRegister\",\"project_name\":\"MyProject\",\"object_name\":\"Document.Receipt\",\"related_object_name\":\"AccumulationRegister.Stock\"}"); //$NON-NLS-1$
        add("removeDocumentRegister", "Removes a register from a document's register records.", //$NON-NLS-1$ //$NON-NLS-2$
            set("project_name", "object_name", "related_object_name"), set("dry_run"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            "{\"operation\":\"removeDocumentRegister\",\"project_name\":\"MyProject\",\"object_name\":\"Document.Receipt\",\"related_object_name\":\"AccumulationRegister.Stock\"}"); //$NON-NLS-1$
        add("createObjectForm", "Creates a generated object, list, record, report, or constants form with a persisted Form.form body.", //$NON-NLS-1$ //$NON-NLS-2$
            set("project_name", "object_name", "name", "form_type"), set("title", "dry_run"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
            "{\"operation\":\"createObjectForm\",\"project_name\":\"MyProject\",\"object_name\":\"Catalog.Products\",\"name\":\"ItemForm\",\"form_type\":\"OBJECT\"}"); //$NON-NLS-1$
        add("removeObjectForm", "Removes an object form and its external Form.form body.", //$NON-NLS-1$ //$NON-NLS-2$
            set("project_name", "object_name", "name"), set("dry_run"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            "{\"operation\":\"removeObjectForm\",\"project_name\":\"MyProject\",\"object_name\":\"Catalog.Products\",\"name\":\"ItemForm\"}"); //$NON-NLS-1$
        add("createObjectTemplate", "Creates a persisted empty spreadsheet or data-composition template.", //$NON-NLS-1$ //$NON-NLS-2$
            set("project_name", "object_name", "name", "template_type"), set("title", "dry_run"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
            "{\"operation\":\"createObjectTemplate\",\"project_name\":\"MyProject\",\"object_name\":\"Report.Sales\",\"name\":\"MainDcs\",\"template_type\":\"DATA_COMPOSITION_SCHEMA\"}"); //$NON-NLS-1$
        add("removeObjectTemplate", "Removes an object template and its external body.", //$NON-NLS-1$ //$NON-NLS-2$
            set("project_name", "object_name", "name"), set("dry_run"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            "{\"operation\":\"removeObjectTemplate\",\"project_name\":\"MyProject\",\"object_name\":\"Report.Sales\",\"name\":\"MainDcs\"}"); //$NON-NLS-1$
        add("addSubordinateObject", //$NON-NLS-1$
            "Adds a nested subordinate object to an owner. subordinate_kind: recalculation (owner CalculationRegister) or integration_service_channel (owner IntegrationService).", //$NON-NLS-1$
            set("project_name", "object_name", "subordinate_kind", "name"), set("title", "dry_run"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
            "{\"operation\":\"addSubordinateObject\",\"project_name\":\"MyProject\",\"object_name\":\"CalculationRegister.Payroll\",\"subordinate_kind\":\"recalculation\",\"name\":\"Main\"}"); //$NON-NLS-1$
        add("removeSubordinateObject", "Removes a nested subordinate object from its owner.", //$NON-NLS-1$ //$NON-NLS-2$
            set("project_name", "object_name", "subordinate_kind", "name"), set("dry_run"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            "{\"operation\":\"removeSubordinateObject\",\"project_name\":\"MyProject\",\"object_name\":\"CalculationRegister.Payroll\",\"subordinate_kind\":\"recalculation\",\"name\":\"Main\"}"); //$NON-NLS-1$
        add("createConfiguration", //$NON-NLS-1$
            "Creates a new EDT 1C configuration project. project_name is the new project name. Optional parameters set" //$NON-NLS-1$
                + " configuration properties: platform_version (runtime version), compatibility_mode (e.g. 8.3.24)," //$NON-NLS-1$
                + " script_variant (English or Russian), default_language_code (e.g. ru) and default_language_name" //$NON-NLS-1$
                + " (creates the language and marks it default), version, vendor, and title (synonym). If the user did" //$NON-NLS-1$
                + " not state these, ask them with the AskUser tool before creating; do not silently guess.", //$NON-NLS-1$
            set("project_name"), //$NON-NLS-1$
            set("platform_version", "compatibility_mode", "script_variant", "default_language_code", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                "default_language_name", "version", "vendor", "title", "dry_run"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            "{\"operation\":\"createConfiguration\",\"project_name\":\"MyConfig\",\"platform_version\":\"8.3.24\"," //$NON-NLS-1$
                + "\"compatibility_mode\":\"8.3.24\",\"script_variant\":\"Russian\",\"default_language_code\":\"ru\"}"); //$NON-NLS-1$
        add("removeConfiguration", //$NON-NLS-1$
            "Removes a configuration project from the workspace (source files are kept on disk).", //$NON-NLS-1$
            set("project_name"), set("dry_run"), //$NON-NLS-1$ //$NON-NLS-2$
            "{\"operation\":\"removeConfiguration\",\"project_name\":\"MyConfig\"}"); //$NON-NLS-1$
        add("listModules", //$NON-NLS-1$
            "Lists the BSL code modules an object supports, with each module_kind, its .bsl file path, and whether it exists. Use object_name=Configuration for application-level modules.", //$NON-NLS-1$
            set("project_name", "object_name"), set(), //$NON-NLS-1$ //$NON-NLS-2$
            "{\"operation\":\"listModules\",\"project_name\":\"MyProject\",\"object_name\":\"Catalog.Products\"}"); //$NON-NLS-1$
        add("createModule", //$NON-NLS-1$
            "Creates an empty BSL code module (.bsl file) for the given module_kind, then edit its text with the Edit tool. module_kind values: object_module, manager_module, record_set_module, value_manager_module, command_module, module (CommonModule/HTTPService/WebService/IntegrationService/Bot/CommonForm), managed_application_module, ordinary_application_module, external_connection_module, session_module. Use listModules to see the kinds an object supports.", //$NON-NLS-1$
            set("project_name", "object_name", "module_kind"), set("dry_run"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            "{\"operation\":\"createModule\",\"project_name\":\"MyProject\",\"object_name\":\"Catalog.Products\",\"module_kind\":\"object_module\"}"); //$NON-NLS-1$
        add("removeModule", //$NON-NLS-1$
            "Deletes a BSL code module (.bsl file) of the given module_kind. The object's other modules are left intact.", //$NON-NLS-1$
            set("project_name", "object_name", "module_kind"), set("dry_run"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            "{\"operation\":\"removeModule\",\"project_name\":\"MyProject\",\"object_name\":\"Catalog.Products\",\"module_kind\":\"object_module\"}"); //$NON-NLS-1$
    }

    Collection<MetadataOperationDescriptor> all()
    {
        return Collections.unmodifiableCollection(operations.values());
    }

    MetadataOperationDescriptor get(String name)
    {
        return operations.get(name);
    }

    public Set<String> names()
    {
        return Collections.unmodifiableSet(operations.keySet());
    }

    public List<String> validate(JsonObject arguments)
    {
        var errors = new ArrayList<String>();
        var operationElement = arguments.get("operation"); //$NON-NLS-1$
        if (operationElement == null || !operationElement.isJsonPrimitive()
            || operationElement.getAsString().isBlank())
        {
            errors.add("Missing required parameter `operation`."); //$NON-NLS-1$
            return errors;
        }

        var operation = operationElement.getAsString();
        var descriptor = operations.get(operation);
        if (descriptor == null)
        {
            errors.add("Unknown operation `" + operation + "`. Valid operations: " //$NON-NLS-1$ //$NON-NLS-2$
                + String.join(", ", operations.keySet()) + "."); //$NON-NLS-1$ //$NON-NLS-2$
            return errors;
        }

        for (var parameter : descriptor.requiredParameters)
        {
            var value = arguments.get(parameter);
            if (value == null || value.isJsonNull()
                || value.isJsonPrimitive() && value.getAsJsonPrimitive().isString() && value.getAsString().isBlank())
            {
                errors.add("Missing required parameter `" + parameter + "` for operation `" + operation + "`."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }
        }

        var valid = descriptor.allParameters();
        for (var parameter : arguments.keySet())
        {
            if (!valid.contains(parameter))
            {
                errors.add("Unknown parameter `" + parameter + "` for operation `" + operation //$NON-NLS-1$ //$NON-NLS-2$
                    + "`. Valid parameters: " + String.join(", ", valid) + "."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }
        }
        return errors;
    }

    private void addTypedChild(String name, String description)
    {
        var objectName = name.contains("TabularSection") ? "Document.Order.Lines" : "Catalog.Products"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        add(name, description, set("project_name", "object_name", "name", "type"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            union(TYPE_PARAMETERS, set("title", "dry_run")), //$NON-NLS-1$ //$NON-NLS-2$
            "{\"operation\":\"" + name //$NON-NLS-1$
                + "\",\"project_name\":\"MyProject\",\"object_name\":\"" + objectName //$NON-NLS-1$
                + "\",\"name\":\"Article\",\"type\":\"String\",\"length\":20}"); //$NON-NLS-1$
    }

    private void addRemoveChild(String name, String description)
    {
        var objectName = name.contains("TabularSectionAttribute") //$NON-NLS-1$
            ? "Document.Order.Lines" : "Catalog.Products"; //$NON-NLS-1$ //$NON-NLS-2$
        add(name, description, set("project_name", "object_name", "name"), set("dry_run"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            "{\"operation\":\"" + name //$NON-NLS-1$
                + "\",\"project_name\":\"MyProject\",\"object_name\":\"" + objectName //$NON-NLS-1$
                + "\",\"name\":\"Article\"}"); //$NON-NLS-1$
    }

    private void add(String name, String description, Set<String> required, Set<String> optional, String example)
    {
        operations.put(name, new MetadataOperationDescriptor(name, description, required, optional, example));
    }

    private static Set<String> set(String... values)
    {
        return new LinkedHashSet<>(Arrays.asList(values));
    }

    private static Set<String> union(Set<String> left, Set<String> right)
    {
        var result = new LinkedHashSet<>(left);
        result.addAll(right);
        return result;
    }
}
