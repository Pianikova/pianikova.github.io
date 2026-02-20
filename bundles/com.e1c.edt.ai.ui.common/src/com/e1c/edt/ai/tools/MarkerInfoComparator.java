/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.util.Comparator;

import com.e1c.edt.ai.assistent.model.MarkerInfo;

/**
 * Comparator for {@link MarkerInfo} that sorts markers by importance.
 * <p>
 * Sorting order:
 * <ul>
 * <li>First by severity (error > warning > info) in descending order</li>
 * <li>Then by priority (high > normal > low) in descending order</li>
 * </ul>
 */
public class MarkerInfoComparator implements Comparator<MarkerInfo>
{

	@Override
	public int compare(MarkerInfo m1, MarkerInfo m2)
	{
		// Compare by severity first (error > warning > info)
		int severityCompare = compareSeverity(m1.severity, m2.severity);
		if (severityCompare != 0)
		{
			return severityCompare;
		}

		// If severity is equal, compare by priority (high > normal > low)
		return comparePriority(m1.priority, m2.priority);
	}

	private int compareSeverity(String severity1, String severity2)
	{
		int severityValue1 = getSeverityValue(severity1);
		int severityValue2 = getSeverityValue(severity2);
		return Integer.compare(severityValue2, severityValue1); // Descending order (higher value first)
	}

	private int getSeverityValue(String severity)
	{
        if (MarkerInfo.SEVERITY_ERROR.equals(severity))
		{
			return 3;
		}
        else if (MarkerInfo.SEVERITY_WARNING.equals(severity))
		{
			return 2;
		}
        else if (MarkerInfo.SEVERITY_INFO.equals(severity))
		{
			return 1;
		}
		return 0; // No severity or null
	}

	private int comparePriority(String priority1, String priority2)
	{
		int priorityValue1 = getPriorityValue(priority1);
		int priorityValue2 = getPriorityValue(priority2);
		return Integer.compare(priorityValue2, priorityValue1); // Descending order (higher value first)
	}

	private int getPriorityValue(String priority)
	{
        if (MarkerInfo.PRIORITY_HIGH.equals(priority))
		{
			return 3;
		}
        else if (MarkerInfo.PRIORITY_NORMAL.equals(priority))
		{
			return 2;
		}
        else if (MarkerInfo.PRIORITY_LOW.equals(priority))
		{
			return 1;
		}
		return 0; // No priority or null
	}
}
