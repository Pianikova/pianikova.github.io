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
        add("inspectObject", //$NON-NLS-1$
            "Reads an object, its scalar properties, children, types, forms, templates, and register links." //$NON-NLS-1$
                + " Also returns vendor_support {object_belonging, configuration_on_full_support, editable," //$NON-NLS-1$
                + " deletable}: when editable is false the object is on vendor support and must be left alone," //$NON-NLS-1$
                + " so check it before planning changes to an unfamiliar configuration.", //$NON-NLS-1$
            set("project_name", "object_name"), set(), //$NON-NLS-1$ //$NON-NLS-2$
            "{\"operation\":\"inspectObject\",\"project_name\":\"MyProject\",\"object_name\":\"Document.Order\"}"); //$NON-NLS-1$
        add("createObject", "Creates any top-level object type listed by help topic=objectTypes.", //$NON-NLS-1$ //$NON-NLS-2$
            set("project_name", "object_name"), //$NON-NLS-1$ //$NON-NLS-2$
            set("title", "name", "dry_run"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "{\"operation\":\"createObject\",\"project_name\":\"MyProject\",\"object_name\":\"Catalog.Products\",\"title\":\"Products\"}"); //$NON-NLS-1$
        add("setObjectProperty", //$NON-NLS-1$
            "Changes one property of an existing top-level object. Use object_name=Configuration to edit" //$NON-NLS-1$
                + " configuration properties such as version, compatibilityMode, scriptVariant, vendor," //$NON-NLS-1$
                + " defaultRunMode, or namePrefix. Also sets single-valued reference properties that some object" //$NON-NLS-1$
                + " types require: pass the target FQN as property_value, for example property_name=task with" //$NON-NLS-1$
                + " property_value=Task.MyTask for a BusinessProcess, or chartOfCalculationTypes for a" //$NON-NLS-1$
                + " CalculationRegister. For a collection of references use addObjectReference instead." //$NON-NLS-1$
                + " Multilingual properties (synonym, listPresentation, objectPresentation," //$NON-NLS-1$
                + " recordPresentation, explanation, ...) are set the same way; language_code picks the" //$NON-NLS-1$
                + " language and defaults to ru. Which of them an object type has differs by type, and a" //$NON-NLS-1$
                + " rejected property_name reports the exact list.", //$NON-NLS-1$
            set("project_name", "object_name", "property_name", "property_value"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            set("language_code", "dry_run"), //$NON-NLS-1$ //$NON-NLS-2$
            "{\"operation\":\"setObjectProperty\",\"project_name\":\"MyProject\",\"object_name\":\"Catalog.Products\",\"property_name\":\"comment\",\"property_value\":\"Managed by AI\"}"); //$NON-NLS-1$
        add("setScheduledJobSchedule", //$NON-NLS-1$
            "Sets a ScheduledJob's persisted daily schedule. Use this operation instead of setObjectProperty:" //$NON-NLS-1$
                + " schedule is a structured EDT model object, not an object reference or JSON scalar." //$NON-NLS-1$
                + " begin_time is 24-hour HH:mm; days_repeat_period defaults to 1.", //$NON-NLS-1$
            set("project_name", "object_name", "begin_time"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            set("days_repeat_period", "dry_run"), //$NON-NLS-1$ //$NON-NLS-2$
            "{\"operation\":\"setScheduledJobSchedule\",\"project_name\":\"MyProject\"," //$NON-NLS-1$
                + "\"object_name\":\"ScheduledJob.DailyJob\",\"begin_time\":\"07:00\"," //$NON-NLS-1$
                + "\"days_repeat_period\":1}"); //$NON-NLS-1$
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

        add("setChildProperty", //$NON-NLS-1$
            "Changes a scalar or multilingual property of an existing child, for example comment," //$NON-NLS-1$
                + " synonym, toolTip or fillValue. language_code picks the language of a multilingual" //$NON-NLS-1$
                + " value and defaults to ru.", //$NON-NLS-1$
            set("project_name", "object_name", "child_kind", "name", "property_name", "property_value"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
            set("language_code", "dry_run"), //$NON-NLS-1$ //$NON-NLS-2$
            "{\"operation\":\"setChildProperty\",\"project_name\":\"MyProject\",\"object_name\":\"Document.Order.Lines\",\"child_kind\":\"tabular_section_attribute\",\"name\":\"Quantity\",\"property_name\":\"comment\",\"property_value\":\"Ordered quantity\"}"); //$NON-NLS-1$
        add("setChildType", "Changes the type of an existing attribute or register field.", //$NON-NLS-1$ //$NON-NLS-2$
            set("project_name", "object_name", "child_kind", "name", "type"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            union(TYPE_PARAMETERS, set("dry_run")), //$NON-NLS-1$
            "{\"operation\":\"setChildType\",\"project_name\":\"MyProject\",\"object_name\":\"Document.Order.Lines\",\"child_kind\":\"tabular_section_attribute\",\"name\":\"Quantity\",\"type\":\"Number\",\"length\":15,\"precision\":3}"); //$NON-NLS-1$
        add("renameChild", "Renames an existing child metadata element.", //$NON-NLS-1$ //$NON-NLS-2$
            set("project_name", "object_name", "child_kind", "name", "new_name"), set("dry_run"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
            "{\"operation\":\"renameChild\",\"project_name\":\"MyProject\",\"object_name\":\"Document.Order\",\"child_kind\":\"tabular_section\",\"name\":\"Lines\",\"new_name\":\"Goods\"}"); //$NON-NLS-1$
        add("addObjectReference", //$NON-NLS-1$
            "Adds an object to a reference collection of another object, for the collections some types require:" //$NON-NLS-1$
                + " for example property_name=registeredDocuments with related_object_name=Document.MyDoc for a" //$NON-NLS-1$
                + " DocumentJournal, or property_name=content on a Subsystem to put an object into that subsystem" //$NON-NLS-1$
                + " (the collection lives on the subsystem, not on the object). property_name is the model" //$NON-NLS-1$
                + " property; related_object_name is the FQN to add. Call inspectObject when unsure of the name.", //$NON-NLS-1$
            set("project_name", "object_name", "property_name", "related_object_name"), set("dry_run"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            "{\"operation\":\"addObjectReference\",\"project_name\":\"MyProject\",\"object_name\":\"DocumentJournal.Journal\",\"property_name\":\"registeredDocuments\",\"related_object_name\":\"Document.Order\"}"); //$NON-NLS-1$
        add("removeObjectReference", "Removes an object from a reference collection of another object.", //$NON-NLS-1$ //$NON-NLS-2$
            set("project_name", "object_name", "property_name", "related_object_name"), set("dry_run"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            "{\"operation\":\"removeObjectReference\",\"project_name\":\"MyProject\",\"object_name\":\"DocumentJournal.Journal\",\"property_name\":\"registeredDocuments\",\"related_object_name\":\"Document.Order\"}"); //$NON-NLS-1$
        add("addDocumentRegister", "Adds an accumulation, accounting, or calculation register to a document's register records.", //$NON-NLS-1$ //$NON-NLS-2$
            set("project_name", "object_name", "related_object_name"), set("dry_run"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            "{\"operation\":\"addDocumentRegister\",\"project_name\":\"MyProject\",\"object_name\":\"Document.Receipt\",\"related_object_name\":\"AccumulationRegister.Stock\"}"); //$NON-NLS-1$
        add("removeDocumentRegister", "Removes a register from a document's register records.", //$NON-NLS-1$ //$NON-NLS-2$
            set("project_name", "object_name", "related_object_name"), set("dry_run"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            "{\"operation\":\"removeDocumentRegister\",\"project_name\":\"MyProject\",\"object_name\":\"Document.Receipt\",\"related_object_name\":\"AccumulationRegister.Stock\"}"); //$NON-NLS-1$
        add("createObjectForm", //$NON-NLS-1$
            "Creates a generated form with a persisted Form.form body and makes it the owner's default form of that" //$NON-NLS-1$
                + " kind when such a slot is free. form_type values: OBJECT, LIST, FOLDER, CHOICE, FOLDER_CHOICE," //$NON-NLS-1$
                + " RECORD, RECORD_SET, REPORT, CONSTANTS, GENERIC, SEARCH, REPORT_SETTINGS, REPORT_VARIANT, SAVE," //$NON-NLS-1$
                + " LOAD, DYNAMIC_LIST, CHANGE_HISTORY, VERSION_DATA, VERSION_DIFFERENCES. Use GENERIC for a new" //$NON-NLS-1$
                + " empty DataProcessor form (or any form you will assemble with addFormAttribute/addFormField/" //$NON-NLS-1$
                + " addFormGroup/addFormCommand/addFormButton); GENERIC does not invoke EDT's layout generator.", //$NON-NLS-1$
            set("project_name", "object_name", "name", "form_type"), set("title", "dry_run"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
            "{\"operation\":\"createObjectForm\",\"project_name\":\"MyProject\",\"object_name\":\"Catalog.Products\",\"name\":\"ItemForm\",\"form_type\":\"OBJECT\"}"); //$NON-NLS-1$
        add("removeObjectForm", "Removes an object form and its external Form.form body.", //$NON-NLS-1$ //$NON-NLS-2$
            set("project_name", "object_name", "name"), set("dry_run"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            "{\"operation\":\"removeObjectForm\",\"project_name\":\"MyProject\",\"object_name\":\"Catalog.Products\",\"name\":\"ItemForm\"}"); //$NON-NLS-1$
        addFormOperations();
        add("createObjectTemplate", //$NON-NLS-1$
            "Creates a persisted empty template and returns the exact body file in details.body_path." //$NON-NLS-1$
                + " template_type values: SPREADSHEET_DOCUMENT, DATA_COMPOSITION_SCHEMA," //$NON-NLS-1$
                + " DATA_COMPOSITION_APPEARANCE_TEMPLATE, HTML_DOCUMENT. An HTML body can then be filled with the" //$NON-NLS-1$
                + " Edit tool. Types wrapping external content (TEXT_DOCUMENT, BINARY_DATA, ADD_IN, ACTIVE_DOCUMENT," //$NON-NLS-1$
                + " GRAPHICAL_SCHEMA, GEOGRAPHICAL_SCHEMA) cannot be created empty.", //$NON-NLS-1$
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
            "Creates a new EDT 1C configuration project. Unlike every other operation, project_name here" //$NON-NLS-1$
                + " names the configuration to CREATE, not an existing project, and it must not exist yet;" //$NON-NLS-1$
                + " there is no separate `name` parameter. Optional parameters set" //$NON-NLS-1$
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
            "Lists the BSL code modules an object supports: each module_kind, the exact .bsl file path, and whether" //$NON-NLS-1$
                + " that file exists. This is the only module operation here, because a 1C module *is* its .bsl file:" //$NON-NLS-1$
                + " create it with the Write tool at the reported path, change its text with Edit, remove it with" //$NON-NLS-1$
                + " Delete. Those tools enforce the vendor-support rules for the owning object." //$NON-NLS-1$
                + " module_kind values: object_module, manager_module, record_set_module, value_manager_module," //$NON-NLS-1$
                + " command_module, module (CommonModule/HTTPService/WebService/IntegrationService/Bot/CommonForm)," //$NON-NLS-1$
                + " managed_application_module, ordinary_application_module, external_connection_module," //$NON-NLS-1$
                + " session_module. Use object_name=Configuration for application-level modules." //$NON-NLS-1$
                + " Nothing is registered in the .mdo for a module: its kind comes from the file name, so for an" //$NON-NLS-1$
                + " existing object writing the .bsl is all that is needed. Two cases need the owner created first:" //$NON-NLS-1$
                + " a common module is a metadata object, so call createObject CommonModule.Name and then write its" //$NON-NLS-1$
                + " Module.bsl (its execution context is set with setObjectProperty on properties such as server," //$NON-NLS-1$
                + " clientManagedApplication, clientOrdinaryApplication, externalConnection, serverCall, global," //$NON-NLS-1$
                + " privileged); a form module requires createObjectForm first. Always call listModules before" //$NON-NLS-1$
                + " writing: it both confirms the owner exists and returns the exact path.", //$NON-NLS-1$
            set("project_name", "object_name"), set(), //$NON-NLS-1$ //$NON-NLS-2$
            "{\"operation\":\"listModules\",\"project_name\":\"MyProject\",\"object_name\":\"Catalog.Products\"}"); //$NON-NLS-1$
    }

    /**
     * Operations on the content of an <em>existing</em> form. They all address the form by its FQN in
     * {@code object_name}, because a form body is a resource of its own and EDT forbids editing the
     * {@code Form.form} file directly; these operations are the only supported way in.
     */
    @SuppressWarnings("nls")
    private void addFormOperations()
    {
        var form = set("project_name", "object_name");
        add("inspectForm",
            "Reads an existing form: its attributes, commands, parameters and the whole item tree with"
                + " names, kinds, data paths and titles. Call it before changing a form you did not create:"
                + " every other form operation addresses items by the names listed here.",
            form, set(),
            "{\"operation\":\"inspectForm\",\"project_name\":\"MyProject\",\"object_name\":\"Catalog.Products.Form.ItemForm\"}");
        add("addFormAttribute",
            "Adds an attribute (реквизит формы) to an existing form. `type` accepts the same names as an"
                + " object attribute plus the types a metadata object produces (CatalogObject.X,"
                + " DataProcessorObject.X, InformationRegisterRecordSet.X, ...) and platform types such as"
                + " DynamicList, ValueTable, ValueTree. Pass main=true for the form's main attribute.",
            union(form, set("name", "type")),
            union(TYPE_PARAMETERS, set("title", "language_code", "main", "dry_run")),
            "{\"operation\":\"addFormAttribute\",\"project_name\":\"MyProject\",\"object_name\":\"Catalog.Products.Form.ItemForm\",\"name\":\"Комментарий\",\"type\":\"String\",\"length\":200}");
        add("removeFormAttribute", "Removes an attribute from an existing form.",
            union(form, set("name")), set("dry_run"),
            "{\"operation\":\"removeFormAttribute\",\"project_name\":\"MyProject\",\"object_name\":\"Catalog.Products.Form.ItemForm\",\"name\":\"Комментарий\"}");
        add("addFormField",
            "Adds a field (элемент формы) bound to `data_path` — an existing form attribute or one of its"
                + " properties, for example Объект.Наименование. inspectForm lists the available paths."
                + " `parent` names the group that receives the field; omit it for the form root."
                + " `item_type` overrides the field kind EDT derives (InputField, CheckBoxField,"
                + " LabelField, PictureField, ...).",
            union(form, set("name", "data_path")),
            set("parent", "position", "item_type", "title", "language_code", "dry_run"),
            "{\"operation\":\"addFormField\",\"project_name\":\"MyProject\",\"object_name\":\"Catalog.Products.Form.ItemForm\",\"name\":\"Комментарий\",\"data_path\":\"Объект.Комментарий\",\"parent\":\"ГруппаОсновное\"}");
        add("addFormGroup",
            "Adds a group to an existing form. group_type: UsualGroup (обычная группа), Pages (страницы),"
                + " Page (страница, parent must be a Pages group), CommandBar, ButtonGroup, ColumnGroup,"
                + " Popup. A page therefore needs a Pages group created first.",
            union(form, set("name", "group_type")),
            set("parent", "position", "title", "language_code", "dry_run"),
            "{\"operation\":\"addFormGroup\",\"project_name\":\"MyProject\",\"object_name\":\"Catalog.Products.Form.ItemForm\",\"name\":\"Страницы\",\"group_type\":\"Pages\"}");
        add("addFormButton",
            "Adds a button bound to an existing form command named by `command_name`. Create the command"
                + " with addFormCommand first. Put the button into a CommandBar group with `parent` to make"
                + " it appear on the command bar.",
            union(form, set("name", "command_name")),
            set("parent", "position", "title", "language_code", "dry_run"),
            "{\"operation\":\"addFormButton\",\"project_name\":\"MyProject\",\"object_name\":\"Catalog.Products.Form.ItemForm\",\"name\":\"ЗаполнитьПоУмолчанию\",\"command_name\":\"ЗаполнитьПоУмолчанию\",\"parent\":\"ФормаКоманднаяПанель\"}");
        add("addFormCommand",
            "Adds a form command (команда формы) and points it at a form-module procedure. `handler`"
                + " defaults to the command name. A 1C command is incomplete without that procedure, so"
                + " the form keeps an error marker until you write it: the response returns the exact"
                + " file in details.handler_module_path, and BSL is written with Write/Edit, not with this"
                + " tool. A command becomes visible only through addFormButton.",
            union(form, set("name")),
            set("title", "language_code", "handler", "dry_run"),
            "{\"operation\":\"addFormCommand\",\"project_name\":\"MyProject\",\"object_name\":\"Catalog.Products.Form.ItemForm\",\"name\":\"ЗаполнитьПоУмолчанию\",\"title\":\"Заполнить по умолчанию\"}");
        add("removeFormCommand", "Removes a form command and every button bound to it.",
            union(form, set("name")), set("dry_run"),
            "{\"operation\":\"removeFormCommand\",\"project_name\":\"MyProject\",\"object_name\":\"Catalog.Products.Form.ItemForm\",\"name\":\"ЗаполнитьПоУмолчанию\"}");
        add("removeFormItem", "Removes a field, group, button or table from a form, with its children.",
            union(form, set("name")), set("dry_run"),
            "{\"operation\":\"removeFormItem\",\"project_name\":\"MyProject\",\"object_name\":\"Catalog.Products.Form.ItemForm\",\"name\":\"Комментарий\"}");
        add("moveFormItem",
            "Moves an existing form item into `parent` (omit for the form root) at `position`"
                + " (zero-based; omit to append).",
            union(form, set("name")), set("parent", "position", "dry_run"),
            "{\"operation\":\"moveFormItem\",\"project_name\":\"MyProject\",\"object_name\":\"Catalog.Products.Form.ItemForm\",\"name\":\"Комментарий\",\"parent\":\"СтраницаДополнительно\"}");
        add("setFormItemProperty",
            "Changes one property of a form item, form attribute or form command. Multilingual properties"
                + " such as title are written for `language_code` (default ru).",
            union(form, set("name", "property_name", "property_value")), set("language_code", "dry_run"),
            "{\"operation\":\"setFormItemProperty\",\"project_name\":\"MyProject\",\"object_name\":\"Catalog.Products.Form.ItemForm\",\"name\":\"Комментарий\",\"property_name\":\"title\",\"property_value\":\"Комментарий\"}");
        add("setFormProperty",
            "Changes one property of the form itself, for example title, windowOpeningMode or autoTitle.",
            union(form, set("property_name", "property_value")), set("language_code", "dry_run"),
            "{\"operation\":\"setFormProperty\",\"project_name\":\"MyProject\",\"object_name\":\"Catalog.Products.Form.ItemForm\",\"property_name\":\"title\",\"property_value\":\"Товар\"}");
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
        if (!errors.isEmpty())
        {
            errors.add("Correct example for operation `" + operation + "`: " + descriptor.example); //$NON-NLS-1$ //$NON-NLS-2$
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
