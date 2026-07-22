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
        add("createObject", "Creates a top-level 1C metadata object.", set("project_name", "object_name"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            set("title", "dry_run"), //$NON-NLS-1$ //$NON-NLS-2$
            "{\"operation\":\"createObject\",\"project_name\":\"MyProject\",\"object_name\":\"Catalog.Products\",\"title\":\"Products\"}"); //$NON-NLS-1$
        add("setObjectProperty", "Changes one scalar property of an existing top-level object.", //$NON-NLS-1$ //$NON-NLS-2$
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
