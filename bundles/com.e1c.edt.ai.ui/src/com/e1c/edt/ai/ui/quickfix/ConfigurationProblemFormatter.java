/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.ui.quickfix;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com._1c.g5.v8.dt.validation.marker.Marker;

/** Renders EDT validation markers into a bounded, deterministic prompt fragment. */
final class ConfigurationProblemFormatter
{
    private static final int MAX_VALUE_LENGTH = 2_000;
    private static final int MAX_EXTRA_INFO_ENTRIES = 20;

    static String format(List<Marker> markers)
    {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < markers.size(); i++)
        {
            Marker marker = markers.get(i);
            if (i > 0)
            {
                result.append('\n');
            }
            result.append("Problem ").append(i + 1).append(":\n"); //$NON-NLS-1$ //$NON-NLS-2$
            append(result, "message", marker.getMessage()); //$NON-NLS-1$
            append(result, "severity", marker.getSeverity()); //$NON-NLS-1$
            append(result, "check_id", marker.getCheckId()); //$NON-NLS-1$
            append(result, "source_type", marker.getSourceType()); //$NON-NLS-1$
            append(result, "marker_id", marker.getMarkerId()); //$NON-NLS-1$
            append(result, "source_object_id", marker.getSourceObjectId()); //$NON-NLS-1$
            append(result, "feature_id", marker.getFeatureId()); //$NON-NLS-1$
            appendExtraInfo(result, getExtraInfo(marker));
        }
        return result.toString();
    }

    private static void appendExtraInfo(StringBuilder result, Map<String, String> extraInfo)
    {
        if (extraInfo == null || extraInfo.isEmpty())
        {
            return;
        }
        result.append("extra_info:\n"); //$NON-NLS-1$
        extraInfo.entrySet().stream()
            .sorted(Map.Entry.comparingByKey(Comparator.nullsFirst(Comparator.naturalOrder())))
            .limit(MAX_EXTRA_INFO_ENTRIES)
            .forEach(entry -> result.append("  ") //$NON-NLS-1$
                .append(value(entry.getKey()))
                .append(": ") //$NON-NLS-1$
                .append(value(entry.getValue()))
                .append('\n'));
    }

    /**
     * Marker API 8 returns {@link Map}, while API 9 returns a covariant IExtraInfoMap. Calling the
     * method directly would encode the return type into the JVM descriptor and fail with
     * NoSuchMethodError on the other API version.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, String> getExtraInfo(Marker marker)
    {
        try
        {
            Object result = marker.getClass().getMethod("getExtraInfo").invoke(marker); //$NON-NLS-1$
            return result instanceof Map<?, ?> ? (Map<String, String>)result : Map.of();
        }
        catch (ReflectiveOperationException | RuntimeException e)
        {
            return Map.of();
        }
    }

    private static void append(StringBuilder result, String name, Object rawValue)
    {
        result.append(name).append(": ").append(value(rawValue)).append('\n'); //$NON-NLS-1$
    }

    private static String value(Object rawValue)
    {
        String text = rawValue == null ? "" : String.valueOf(rawValue); //$NON-NLS-1$
        text = text.replace("\r", "\\r").replace("\n", "\\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        return text.length() <= MAX_VALUE_LENGTH ? text : text.substring(0, MAX_VALUE_LENGTH) + "..."; //$NON-NLS-1$
    }

    private ConfigurationProblemFormatter()
    {
    }
}
