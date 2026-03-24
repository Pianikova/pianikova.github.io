/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import com.google.inject.Singleton;

/**
 * Provider of restricted types based on configuration file.
 *
 * This provider loads restricted types from a properties file and makes them
 * available for use by the RestrictedTypesValidator.
 */
@Singleton
public class RestrictedTypesProvider
	implements IRestrictedTypesProvider
{
    private static final String RESTRICTED_TYPES_FILE = "restricted-types.properties"; //$NON-NLS-1$
	private final Set<String> restrictedTypes;

	/**
	 * Creates a restricted types provider and loads configuration from the properties file.
	 */
	public RestrictedTypesProvider()
	{
		this.restrictedTypes = loadRestrictedTypes();
	}

	@Override
	public Set<String> getRestrictedTypes()
	{
		return Collections.unmodifiableSet(restrictedTypes);
	}

    @SuppressWarnings("nls")
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

	/**
	 * Loads restricted types from the properties file.
	 *
	 * @return Set of restricted type names
	 */
	private Set<String> loadRestrictedTypes()
	{
		var types = new HashSet<String>();
		var properties = new Properties();

		try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(RESTRICTED_TYPES_FILE))
		{
			if (inputStream == null)
			{
				// File not found, return empty set
				return types;
			}

			properties.load(inputStream);

			for (String key : properties.stringPropertyNames())
			{
				// Skip comments (keys starting with #)
                if (key != null && !key.trim().isEmpty() && !key.trim().startsWith("#")) //$NON-NLS-1$
				{
					types.add(key.trim());
				}
			}
		}
		catch (IOException e)
		{
            // Log warning and continue with empty set
		}

		return types;
	}
}
