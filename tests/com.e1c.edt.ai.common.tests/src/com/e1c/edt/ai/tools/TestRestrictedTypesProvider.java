/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.util.HashSet;
import java.util.Set;

/**
 * Test implementation of IRestrictedTypesProvider for unit tests.
 */
class TestRestrictedTypesProvider
	implements IRestrictedTypesProvider
{
	private final Set<String> restrictedTypes;

	public TestRestrictedTypesProvider(Set<String> restrictedTypes)
	{
		this.restrictedTypes = restrictedTypes != null ? restrictedTypes : new HashSet<>();
	}

	@Override
	public Set<String> getRestrictedTypes()
	{
		return restrictedTypes;
	}

	@Override
	public boolean isRestricted(String typeName)
	{
		if (typeName == null)
		{
			return false;
		}

		// Check exact match
		if (restrictedTypes.contains(typeName))
		{
			return true;
		}

		// Check for package prefixes (e.g., "java.io.*")
		for (String restricted : restrictedTypes)
		{
			if (restricted.endsWith(".*"))
			{
				String packagePrefix = restricted.substring(0, restricted.length() - 1);
				if (typeName.startsWith(packagePrefix))
				{
					return true;
				}
			}
		}

		return false;
	}
}
