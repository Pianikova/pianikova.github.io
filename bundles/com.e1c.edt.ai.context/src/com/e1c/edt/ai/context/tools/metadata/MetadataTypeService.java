/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context.tools.metadata;

import org.eclipse.core.resources.IProject;

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

        var dependencyFqn = referenceDependency(normalized);
        if (dependencyFqn != null)
        {
            var dependency = transaction.getTopObjectByFqn(dependencyFqn);
            if (!(dependency instanceof MdObject))
            {
                throw new ToolException("Referenced metadata object not found: " + dependencyFqn); //$NON-NLS-1$
            }
            var producedType = MdProducedTypesUtil.getProducedType((MdObject)dependency,
                MdTypePackage.Literals.MD_REF_TYPE);
            if (producedType == null)
            {
                throw new ToolException("Cannot resolve produced reference type: " + normalized); //$NON-NLS-1$
            }
            return builder.addType(producedType).build();
        }

        throw new ToolException("Unsupported type `" + normalized //$NON-NLS-1$
            + "`. Supported types: " + supportedTypeNames() + "."); //$NON-NLS-1$ //$NON-NLS-2$
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
     * Reference type prefixes. The rule is uniform: {@code <Type>Ref.<Name>} designates the object
     * {@code <Type>.<Name>}, whose reference type EDT then produces, so a new referenceable kind only
     * needs its prefix here.
     */
    @SuppressWarnings("nls")
    private static final String[] REFERENCE_PREFIXES = { "CatalogRef.", "DocumentRef.", "EnumRef.",
        "ChartOfCharacteristicTypesRef.", "ChartOfAccountsRef.", "ChartOfCalculationTypesRef.",
        "BusinessProcessRef.", "TaskRef.", "ExchangePlanRef." };

    static String supportedTypeNames()
    {
        var names = new StringBuilder("String, Number, Boolean, Date"); //$NON-NLS-1$
        for (var prefix : REFERENCE_PREFIXES)
        {
            names.append(", ").append(prefix).append('X'); //$NON-NLS-1$
        }
        return names.toString();
    }

    private static String referenceDependency(String typeName)
    {
        for (var prefix : REFERENCE_PREFIXES)
        {
            if (typeName.regionMatches(true, 0, prefix, 0, prefix.length()) && typeName.length() > prefix.length())
            {
                return prefix.substring(0, prefix.length() - 4) + "." + typeName.substring(prefix.length()); //$NON-NLS-1$
            }
        }
        return null;
    }

    private static boolean equals(String left, String right)
    {
        return left.equalsIgnoreCase(right);
    }
}
