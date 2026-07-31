/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context.tools.metadata;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.emf.ecore.EClass;

import com._1c.g5.v8.bm.core.IBmTransaction;
import com._1c.g5.v8.dt.core.platform.IV8ProjectManager;
import com._1c.g5.v8.dt.mcore.DateFractions;
import com._1c.g5.v8.dt.mcore.McorePackage;
import com._1c.g5.v8.dt.mcore.TypeDescription;
import com._1c.g5.v8.dt.mcore.TypeItem;
import com._1c.g5.v8.dt.metadata.mdclass.MdObject;
import com._1c.g5.v8.dt.metadata.mdclass.util.MdProducedTypesUtil;
import com._1c.g5.v8.dt.metadata.mdtype.MdTypePackage;
import com._1c.g5.v8.dt.platform.IEObjectProvider;
import com._1c.g5.v8.dt.platform.IEObjectTypeNames;
import com._1c.g5.v8.dt.platform.core.typeinfo.TypeDescriptionBuilder;
import com.e1c.edt.ai.ToolException;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
final class MetadataTypeService
{
    private final IV8ProjectManager projectManager;

    @Inject
    MetadataTypeService(IV8ProjectManager projectManager)
    {
        Preconditions.checkNotNull(projectManager);
        this.projectManager = projectManager;
    }

    TypeDescription create(IProject project, IBmTransaction transaction, MetadataRequest request)
    {
        var typeName = request.type;
        if (typeName == null || typeName.isBlank())
        {
            throw new ToolException("Parameter `type` is required."); //$NON-NLS-1$
        }

        var v8Project = projectManager.getProject(project);
        if (v8Project == null)
        {
            throw new ToolException("V8 project is not available: " + project.getName()); //$NON-NLS-1$
        }

        var provider = IEObjectProvider.Registry.INSTANCE.get(McorePackage.Literals.TYPE_ITEM, v8Project.getVersion());
        if (provider == null)
        {
            throw new ToolException("EDT type provider is not available for project: " + project.getName()); //$NON-NLS-1$
        }

        var normalized = typeName.trim();
        var builder = new TypeDescriptionBuilder();
        if (equals(normalized, "String")) //$NON-NLS-1$
        {
            builder.addType(primitive(provider, IEObjectTypeNames.STRING, normalized));
            int length = request.length != null ? request.length : 100;
            if (length < 0)
            {
                throw new ToolException("String length must be 0 (unlimited) or a positive number."); //$NON-NLS-1$
            }
            return builder.setStringQualifiers(length, false).build();
        }
        if (equals(normalized, "Number")) //$NON-NLS-1$
        {
            builder.addType(primitive(provider, IEObjectTypeNames.NUMBER, normalized));
            int totalDigits = request.length != null ? request.length : 15;
            int scale = request.precision != null ? request.precision : 0;
            if (totalDigits <= 0 || scale < 0 || scale > totalDigits)
            {
                throw new ToolException("Number requires length > 0 and 0 <= precision <= length."); //$NON-NLS-1$
            }
            return builder.setNumberQualifiers(scale, totalDigits, false).build();
        }
        if (equals(normalized, "Boolean")) //$NON-NLS-1$
        {
            return builder.addType(primitive(provider, IEObjectTypeNames.BOOLEAN, normalized)).build();
        }
        if (equals(normalized, "Date")) //$NON-NLS-1$
        {
            builder.addType(primitive(provider, IEObjectTypeNames.DATE, normalized));
            return builder.setDateQualifiers(parseDateFractions(request.dateFractions)).build();
        }

        var produced = producedType(transaction, normalized);
        if (produced != null)
        {
            return builder.addType(produced).build();
        }

        // Any remaining name is looked up as a platform type (DynamicList, ValueTable, ValueTree,
        // ValueList, UUID, ...). Form attributes routinely need those, and the platform type index is
        // the authority on which names exist for this runtime version, so there is nothing to hardcode.
        if (provider.getEObjectDescription(normalized) != null)
        {
            var platformType = provider.getProxy(normalized);
            if (platformType instanceof TypeItem)
            {
                return builder.addType((TypeItem)platformType).build();
            }
        }

        throw new ToolException("Unsupported type `" + normalized //$NON-NLS-1$
            + "`. Supported types: " + supportedTypeNames() + "."); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * Resolves a metadata-produced type such as {@code CatalogRef.X}, {@code CatalogObject.X} or
     * {@code InformationRegisterRecordSet.X}, or {@code null} when the name is not of that shape.
     */
    private static TypeItem producedType(IBmTransaction transaction, String typeName)
    {
        int dot = typeName.indexOf('.');
        if (dot <= 0 || dot == typeName.length() - 1)
        {
            return null;
        }
        var head = typeName.substring(0, dot);
        var objectName = typeName.substring(dot + 1);
        for (var suffix : PRODUCED_TYPE_SUFFIXES.entrySet())
        {
            var key = suffix.getKey();
            if (head.length() <= key.length() || !head.regionMatches(true, head.length() - key.length(), key, 0,
                key.length()))
            {
                continue;
            }
            var ownerFqn = head.substring(0, head.length() - key.length()) + "." + objectName; //$NON-NLS-1$
            var dependency = transaction.getTopObjectByFqn(ownerFqn);
            if (!(dependency instanceof MdObject))
            {
                throw new ToolException("Referenced metadata object not found: " + ownerFqn //$NON-NLS-1$
                    + ". Type `" + typeName + "` names the type produced by that object, so the object must" //$NON-NLS-1$ //$NON-NLS-2$
                    + " exist first."); //$NON-NLS-1$
            }
            var producedType = MdProducedTypesUtil.getProducedType((MdObject)dependency, suffix.getValue());
            if (producedType == null)
            {
                throw new ToolException("Object " + ownerFqn + " does not produce type `" + typeName + "`."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }
            return producedType;
        }
        return null;
    }

    private static TypeItem primitive(IEObjectProvider provider, String typeName, String requestedName)
    {
        var result = provider.getProxy(typeName);
        if (!(result instanceof TypeItem))
        {
            throw new ToolException("Cannot resolve EDT primitive type: " + requestedName); //$NON-NLS-1$
        }
        return (TypeItem)result;
    }

    private static DateFractions parseDateFractions(String value)
    {
        if (value == null || value.isBlank() || equals(value, "DateTime") || equals(value, "Date_Time")) //$NON-NLS-1$ //$NON-NLS-2$
        {
            return DateFractions.DATE_TIME;
        }
        if (equals(value, "Date")) //$NON-NLS-1$
        {
            return DateFractions.DATE;
        }
        if (equals(value, "Time")) //$NON-NLS-1$
        {
            return DateFractions.TIME;
        }
        throw new ToolException("Invalid `date_fractions`. Valid values: Date, Time, DateTime."); //$NON-NLS-1$
    }

    /**
     * Suffixes of the types a metadata object produces, longest first so that {@code RecordManager} is
     * not mistaken for {@code Manager}. The rule is uniform: {@code <Type><Suffix>.<Name>} designates
     * the type produced by object {@code <Type>.<Name>}, so a new object kind needs nothing here.
     */
    private static final Map<String, EClass> PRODUCED_TYPE_SUFFIXES = createProducedTypeSuffixes();

    @SuppressWarnings("nls")
    private static Map<String, EClass> createProducedTypeSuffixes()
    {
        Map<String, EClass> result = new LinkedHashMap<>();
        result.put("RecordManager", MdTypePackage.Literals.MD_RECORD_MANAGER_TYPE);
        result.put("RecordSet", MdTypePackage.Literals.MD_RECORD_SET_TYPE);
        result.put("RecordKey", MdTypePackage.Literals.MD_RECORD_KEY_TYPE);
        result.put("ValueManager", MdTypePackage.Literals.MD_VALUE_MANAGER_TYPE);
        result.put("ValueKey", MdTypePackage.Literals.MD_VALUE_KEY_TYPE);
        result.put("Selection", MdTypePackage.Literals.MD_SELECTION_TYPE);
        result.put("Manager", MdTypePackage.Literals.MD_MANAGER_TYPE);
        result.put("Object", MdTypePackage.Literals.MD_OBJECT_TYPE);
        result.put("List", MdTypePackage.Literals.MD_LIST_TYPE);
        result.put("Ref", MdTypePackage.Literals.MD_REF_TYPE);
        return result;
    }

    static String supportedTypeNames()
    {
        var names = new StringBuilder("String, Number, Boolean, Date"); //$NON-NLS-1$
        for (var suffix : PRODUCED_TYPE_SUFFIXES.keySet())
        {
            names.append(", <ObjectType>").append(suffix).append(".<Name>"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        names.append(", and any platform type name such as DynamicList, ValueTable, ValueTree, ValueList"); //$NON-NLS-1$
        return names.toString();
    }

    private static boolean equals(String left, String right)
    {
        return left.equalsIgnoreCase(right);
    }
}
