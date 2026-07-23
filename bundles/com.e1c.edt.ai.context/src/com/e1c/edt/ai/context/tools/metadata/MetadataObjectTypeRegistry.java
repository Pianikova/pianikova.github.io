/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context.tools.metadata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage;
import com.e1c.edt.ai.ToolException;

public final class MetadataObjectTypeRegistry
{
    private static final String INITIALIZER_PACKAGE = "com._1c.g5.v8.dt.md.model."; //$NON-NLS-1$
    private static final Map<String, ObjectType> TYPES = createTypes();

    private MetadataObjectTypeRegistry()
    {
    }

    public static ObjectType get(String name)
    {
        var result = TYPES.get(name);
        if (result == null)
        {
            throw new ToolException("Unsupported object type `" + name + "`. Supported: " //$NON-NLS-1$ //$NON-NLS-2$
                + String.join(", ", TYPES.keySet()) + "."); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return result;
    }

    public static Collection<ObjectType> all()
    {
        return Collections.unmodifiableCollection(TYPES.values());
    }

    public static List<String> validateEdtModel()
    {
        var errors = new ArrayList<String>();
        for (var type : TYPES.values())
        {
            if (MdClassPackage.eINSTANCE.getEClassifier(type.name) == null)
            {
                errors.add("EDT metadata class is missing: " + type.name); //$NON-NLS-1$
            }
            if (!type.external
                && MdClassPackage.eINSTANCE.getConfiguration().getEStructuralFeature(type.collection) == null)
            {
                errors.add("EDT Configuration collection is missing: " + type.name + ":" + type.collection); //$NON-NLS-1$ //$NON-NLS-2$
            }
            if (type.initializer != null)
            {
                try
                {
                    Class.forName(type.initializer);
                }
                catch (ClassNotFoundException e)
                {
                    errors.add("EDT initializer is missing: " + type.initializer); //$NON-NLS-1$
                }
            }
        }
        return errors;
    }

    private static Map<String, ObjectType> createTypes()
    {
        Map<String, ObjectType> result = new LinkedHashMap<>();
        add(result, "Catalog", "catalogs", "Catalogs", "CatalogInitializer"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(result, "Document", "documents", "Documents", "DocumentInitializer"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(result, "InformationRegister", "informationRegisters", "InformationRegisters", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "InformationRegisterInitializer"); //$NON-NLS-1$
        add(result, "AccumulationRegister", "accumulationRegisters", "AccumulationRegisters", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "AccumulationRegisterInitializer"); //$NON-NLS-1$
        add(result, "AccountingRegister", "accountingRegisters", "AccountingRegisters", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "AccountingRegisterInitializer"); //$NON-NLS-1$
        add(result, "CalculationRegister", "calculationRegisters", "CalculationRegisters", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "CalculationRegisterInitializer"); //$NON-NLS-1$
        add(result, "ChartOfAccounts", "chartsOfAccounts", "ChartsOfAccounts", "ChartOfAccountsInitializer"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(result, "ChartOfCharacteristicTypes", "chartsOfCharacteristicTypes", //$NON-NLS-1$ //$NON-NLS-2$
            "ChartsOfCharacteristicTypes", "ChartOfCharacteristicTypesInitializer"); //$NON-NLS-1$ //$NON-NLS-2$
        add(result, "ChartOfCalculationTypes", "chartsOfCalculationTypes", "ChartsOfCalculationTypes", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "ChartOfCalculationTypesInitializer"); //$NON-NLS-1$
        add(result, "BusinessProcess", "businessProcesses", "BusinessProcesses", "BusinessProcessInitializer"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(result, "Task", "tasks", "Tasks", "TaskInitializer"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(result, "Subsystem", "subsystems", "Subsystems", "SubsystemInitializer"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(result, "Role", "roles", "Roles", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        add(result, "CommonModule", "commonModules", "CommonModules", "CommonModuleInitializer"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(result, "CommonForm", "commonForms", "CommonForms", "CommonFormInitializer"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(result, "CommonCommand", "commonCommands", "CommonCommands", "CommonCommandInitializer"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(result, "CommonAttribute", "commonAttributes", "CommonAttributes", "CommonAttributeInitializer"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(result, "Constant", "constants", "Constants", "ConstantInitializer"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(result, "HTTPService", "httpServices", "HTTPServices", "HttpServiceInitializer"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(result, "WebService", "webServices", "WebServices", "WebServiceInitializer"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(result, "WSReference", "wsReferences", "WSReferences", "WsReferenceInitializer"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(result, "XDTOPackage", "xDTOPackages", "XDTOPackages", "XdtoPackageInitializer"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(result, "Enum", "enums", "Enums", "EnumInitializer"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(result, "Report", "reports", "Reports", "ReportInitializer"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(result, "DataProcessor", "dataProcessors", "DataProcessors", "DataProcessorInitializer"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(result, "ExchangePlan", "exchangePlans", "ExchangePlans", "ExchangePlanInitializer"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(result, "FunctionalOption", "functionalOptions", "FunctionalOptions", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "FunctionalOptionsInitializer"); //$NON-NLS-1$
        add(result, "FunctionalOptionsParameter", "functionalOptionsParameters", //$NON-NLS-1$ //$NON-NLS-2$
            "FunctionalOptionsParameters", "FunctionalOptionsParameterInitializer"); //$NON-NLS-1$ //$NON-NLS-2$
        add(result, "DefinedType", "definedTypes", "DefinedTypes", "DefinedTypeInitializer"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(result, "FilterCriterion", "filterCriteria", "FilterCriteria", "FilterCriterionInitializer"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(result, "SessionParameter", "sessionParameters", "SessionParameters", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        add(result, "EventSubscription", "eventSubscriptions", "EventSubscriptions", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "EventSubscriptionInitializer"); //$NON-NLS-1$
        add(result, "ScheduledJob", "scheduledJobs", "ScheduledJobs", "ScheduledJobInitializer"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(result, "DocumentJournal", "documentJournals", "DocumentJournals", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "DocumentJournalInitializer"); //$NON-NLS-1$
        add(result, "DocumentNumerator", "documentNumerators", "DocumentNumerators", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            "DocumentNumeratorInitializer"); //$NON-NLS-1$
        add(result, "Sequence", "sequences", "Sequences", "SequenceInitializer"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(result, "Style", "styles", "Styles", "StyleInitializer"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(result, "StyleItem", "styleItems", "StyleItems", "StyleItemInitializer"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        // Language is the only configuration type persisted inline in Configuration.mdo rather than
        // as its own <folder>/<name>/<name>.mdo resource, so it is not a standalone BM top object.
        addInline(result, "Language", "languages", "Languages", "LanguageInitializer"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        addExternal(result, "ExternalDataProcessor", "ExternalDataProcessors", //$NON-NLS-1$ //$NON-NLS-2$
            "ExternalDataProcessorInitializer"); //$NON-NLS-1$
        addExternal(result, "ExternalReport", "ExternalReports", "ExternalReportInitializer"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        return result;
    }

    private static void add(Map<String, ObjectType> target, String name, String collection, String folder,
        String initializer)
    {
        target.put(name, new ObjectType(name, collection, folder, initializerName(initializer), false, false));
    }

    private static void addInline(Map<String, ObjectType> target, String name, String collection, String folder,
        String initializer)
    {
        target.put(name, new ObjectType(name, collection, folder, initializerName(initializer), false, true));
    }

    private static void addExternal(Map<String, ObjectType> target, String name, String folder, String initializer)
    {
        target.put(name, new ObjectType(name, null, folder, initializerName(initializer), true, false));
    }

    private static String initializerName(String name)
    {
        return name == null ? null : INITIALIZER_PACKAGE + name;
    }

    public static final class ObjectType
    {
        public final String name;
        public final String collection;
        public final String folder;
        public final String initializer;
        public final boolean external;
        /** True for types stored inline in Configuration.mdo (no own resource, not a BM top object). */
        public final boolean inlineInConfiguration;

        ObjectType(String name, String collection, String folder, String initializer, boolean external,
            boolean inlineInConfiguration)
        {
            this.name = name;
            this.collection = collection;
            this.folder = folder;
            this.initializer = initializer;
            this.external = external;
            this.inlineInConfiguration = inlineInConfiguration;
        }
    }
}
