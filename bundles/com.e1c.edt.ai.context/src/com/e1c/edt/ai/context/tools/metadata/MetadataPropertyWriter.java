/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context.tools.metadata;

import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.emf.common.util.EMap;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.EcoreUtil;

import com.e1c.edt.ai.ToolErrorType;
import com.e1c.edt.ai.ToolException;

/**
 * Writes one scalar or multilingual property of an EDT model object and reports what actually
 * happened.
 * <p>
 * Two properties of 1C metadata make a naive {@code eSet} misleading, and both are handled here:
 * <ul>
 * <li>A multilingual property ({@code synonym}, {@code listPresentation}, {@code objectPresentation},
 * {@code explanation}, ...) is a {@code String -> String} map keyed by language code, not a scalar.
 * Setting it with {@code EcoreUtil.createFromString} is impossible, so those properties used to be
 * unreachable.</li>
 * <li>Writing a value equal to the current one changes nothing. Reporting only {@code changed:false}
 * reads like a silent failure, so the old value and an explicit note go into {@code details}.</li>
 * </ul>
 */
final class MetadataPropertyWriter
{
    /** Language code used when the caller names none. 1C configurations here are Russian-first. */
    static final String DEFAULT_LANGUAGE_CODE = "ru"; //$NON-NLS-1$

    private MetadataPropertyWriter()
    {
    }

    /**
     * Applies {@code request.propertyName = request.propertyValue} to {@code target}.
     *
     * @return {@code true} when the model value actually changed
     */
    static boolean set(EObject target, MetadataRequest request, Map<String, Object> details)
    {
        if (request.propertyName == null || request.propertyName.isBlank())
        {
            throw new ToolException("Parameter `property_name` is required."); //$NON-NLS-1$
        }
        var feature = feature(target, request.propertyName);
        if (isLocalized(feature))
        {
            return setLocalized(target, feature, request, details);
        }
        if (feature.isMany() || !(feature.getEType() instanceof EDataType))
        {
            throw new ToolException("Property `" + request.propertyName + "` of " //$NON-NLS-1$ //$NON-NLS-2$
                + target.eClass().getName() + " is not a scalar." //$NON-NLS-1$
                + (feature.isMany() ? " It holds a collection." : " It holds a model object.") //$NON-NLS-1$ //$NON-NLS-2$
                + " Valid scalar properties: " + scalarPropertyNames(target) + "."); //$NON-NLS-1$ //$NON-NLS-2$
        }
        var oldValue = target.eGet(feature);
        Object newValue;
        try
        {
            newValue = EcoreUtil.createFromString((EDataType)feature.getEType(), request.propertyValue);
        }
        catch (RuntimeException e)
        {
            throw new ToolException("Value `" + request.propertyValue + "` is not valid for property `" //$NON-NLS-1$ //$NON-NLS-2$
                + request.propertyName + "` of type " + feature.getEType().getName() //$NON-NLS-1$
                + validValueHint(feature) + ".", e, ToolErrorType.USER_VISIBLE); //$NON-NLS-1$
        }
        details.put("old_value", String.valueOf(oldValue)); //$NON-NLS-1$
        details.put("new_value", String.valueOf(newValue)); //$NON-NLS-1$
        if (Objects.equals(oldValue, newValue))
        {
            details.put("note", "Property `" + request.propertyName + "` already held this value, so nothing" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                + " was written and the file is unchanged. This is not a failure: re-applying it will not" //$NON-NLS-1$
                + " help. If you expected a different current value, call inspectObject to see the real one."); //$NON-NLS-1$
            return false;
        }
        if (!request.dryRun)
        {
            target.eSet(feature, newValue);
        }
        return true;
    }

    /** True for a {@code String -> String} map feature, which is how EDT stores multilingual text. */
    static boolean isLocalized(EStructuralFeature feature)
    {
        if (!feature.isMany() || !(feature.getEType() instanceof EClass))
        {
            return false;
        }
        var entryClass = (EClass)feature.getEType();
        return "EStringToStringMapEntry".equals(entryClass.getName()) //$NON-NLS-1$
            || entryClass.getEStructuralFeature("key") instanceof EAttribute //$NON-NLS-1$
                && entryClass.getEStructuralFeature("value") instanceof EAttribute //$NON-NLS-1$
                && entryClass.getEStructuralFeatures().size() == 2;
    }

    @SuppressWarnings("unchecked")
    private static boolean setLocalized(EObject target, EStructuralFeature feature, MetadataRequest request,
        Map<String, Object> details)
    {
        var map = (EMap<String, String>)target.eGet(feature);
        var language = request.languageCode != null && !request.languageCode.isBlank()
            ? request.languageCode.trim() : DEFAULT_LANGUAGE_CODE;
        var oldValue = map.get(language);
        details.put("language_code", language); //$NON-NLS-1$
        details.put("old_value", oldValue); //$NON-NLS-1$
        details.put("new_value", request.propertyValue); //$NON-NLS-1$
        if (Objects.equals(oldValue, request.propertyValue))
        {
            details.put("note", "Property `" + request.propertyName + "` already held this value for language `" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                + language + "`, so nothing was written."); //$NON-NLS-1$
            return false;
        }
        if (!request.dryRun)
        {
            map.put(language, request.propertyValue);
        }
        return true;
    }

    private static EStructuralFeature feature(EObject target, String propertyName)
    {
        var feature = target.eClass().getEStructuralFeature(propertyName);
        if (feature != null)
        {
            return feature;
        }
        throw new ToolException("Unsupported property `" + propertyName + "` for " //$NON-NLS-1$ //$NON-NLS-2$
            + target.eClass().getName() + "." + suggestion(target, propertyName) //$NON-NLS-1$
            + " Valid scalar properties: " + scalarPropertyNames(target) //$NON-NLS-1$
            + ". Multilingual properties: " + localizedPropertyNames(target) + "."); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private static String validValueHint(EStructuralFeature feature)
    {
        if (feature.getEType() instanceof org.eclipse.emf.ecore.EEnum)
        {
            var names = new java.util.ArrayList<String>();
            for (var literal : ((org.eclipse.emf.ecore.EEnum)feature.getEType()).getELiterals())
            {
                names.add(literal.getName());
            }
            return ". Valid values: " + String.join(", ", names); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return ""; //$NON-NLS-1$
    }

    static String scalarPropertyNames(EObject target)
    {
        return String.join(", ", collect(target, false)); //$NON-NLS-1$
    }

    static String localizedPropertyNames(EObject target)
    {
        var names = collect(target, true);
        return names.isEmpty() ? "none" : String.join(", ", names); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private static SortedSet<String> collect(EObject target, boolean localized)
    {
        var result = new TreeSet<String>();
        for (var feature : target.eClass().getEAllStructuralFeatures())
        {
            if (localized ? isLocalized(feature)
                : !feature.isMany() && feature.getEType() instanceof EDataType)
            {
                result.add(feature.getName());
            }
        }
        return result;
    }

    /** Closest known property name, so a near miss is corrected in one round instead of guessing. */
    private static String suggestion(EObject target, String requested)
    {
        String best = null;
        for (var feature : target.eClass().getEAllStructuralFeatures())
        {
            var name = feature.getName();
            if (name.equalsIgnoreCase(requested)
                || name.regionMatches(true, 0, requested, 0, Math.min(name.length(), requested.length())))
            {
                if (best == null || Math.abs(name.length() - requested.length()) < Math
                    .abs(best.length() - requested.length()))
                {
                    best = name;
                }
            }
        }
        return best == null ? "" : " Did you mean `" + best + "`?"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }
}
