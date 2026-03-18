/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.e1c.edt.ai.ToolException;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Validates Java code for usage of restricted types.
 *
 * This validator parses Java code to identify potential usage of restricted types
 * before execution in JShell.
 */
@Singleton
public class RestrictedTypesValidator
    implements IRestrictedTypesValidator
{
    private static final Pattern TYPE_IMPORT_PATTERN =
        Pattern.compile("\\bimport\\s+(?:static\\s+)?([\\w\\.\\*]+)\\s*;"); //$NON-NLS-1$
    private static final Pattern TYPE_REFERENCE_PATTERN = Pattern.compile("\\b([\\w.]*\\.[A-Z]\\w*)\\b"); //$NON-NLS-1$
    private static final Pattern NEW_PATTERN = Pattern.compile("\\bnew\\s+([\\w.]*\\.[A-Z]\\w*)\\b"); //$NON-NLS-1$
    private static final Pattern CAST_PATTERN = Pattern.compile("\\(([\\w.]*\\.[A-Z]\\w*)\\)"); //$NON-NLS-1$
    private static final Pattern SIMPLE_TYPE_PATTERN = Pattern.compile("(?<![\\w.])\\b([A-Z][\\w]*)\\b"); //$NON-NLS-1$

	private final Set<String> restrictedTypes;
    private final Map<String, String> simpleNameToFullName;

	/**
     * Creates a validator with the given restricted types provider.
     *
     * @param restrictedTypesProvider Provider of restricted types
     */
    @Inject
    public RestrictedTypesValidator(IRestrictedTypesProvider restrictedTypesProvider)
	{
        this.restrictedTypes = restrictedTypesProvider.getRestrictedTypes();
        this.simpleNameToFullName = buildSimpleNameMap(restrictedTypes);
    }

    /**
     * Builds a map from simple type names to full qualified names.
     *
     * @param restrictedTypes Set of restricted types
     * @return Map of simple names to full names
     */
    @SuppressWarnings("nls")
    private static Map<String, String> buildSimpleNameMap(Set<String> restrictedTypes)
    {
        var map = new HashMap<String, String>();
        for (String type : restrictedTypes)
        {
            if (type != null && !type.endsWith(".*"))
            {
                int lastDot = type.lastIndexOf('.');
                if (lastDot > 0)
                {
                    String simpleName = type.substring(lastDot + 1);
                    map.put(simpleName, type);
                }
            }
        }
        return map;
	}

	/**
	 * Validates code for restricted type usage.
	 *
	 * @param code The Java code to validate
	 * @throws ToolException if a restricted type is found
	 */
	@Override
    @SuppressWarnings("nls")
	public void validate(String code) throws ToolException
	{
		if (code == null || code.isEmpty())
		{
			return;
		}

		String restrictedType = findRestrictedType(code);
		if (restrictedType != null)
		{
			throw new ToolException(
				String.format("Type '%s' is restricted and cannot be used. Please use alternative types that are allowed.",
					restrictedType));
		}
	}

	/**
	 * Searches for restricted types in the given code.
	 *
	 * @param code The Java code to search
	 * @return The first restricted type found, or null if none found
	 */
	private String findRestrictedType(String code)
	{
		// Check import statements
		Matcher importMatcher = TYPE_IMPORT_PATTERN.matcher(code);
		while (importMatcher.find())
		{
			String importType = importMatcher.group(1).trim();
			if (isRestricted(importType))
			{
				return importType;
			}
		}

		// Check for "new" keyword usage
		Matcher newMatcher = NEW_PATTERN.matcher(code);
		while (newMatcher.find())
		{
			String typeName = newMatcher.group(1);
			if (isRestricted(typeName))
			{
				return typeName;
			}
		}

		// Check for cast expressions
		Matcher castMatcher = CAST_PATTERN.matcher(code);
		while (castMatcher.find())
		{
			String typeName = castMatcher.group(1);
			if (isRestricted(typeName))
			{
				return typeName;
			}
		}

		// Check for potential fully qualified type references
		Matcher typeMatcher = TYPE_REFERENCE_PATTERN.matcher(code);
		while (typeMatcher.find())
		{
			String typeName = typeMatcher.group(1);
            if (typeName.contains(".") && isRestricted(typeName)) //$NON-NLS-1$
			{
				return typeName;
			}
		}

        // Check for simple type names (e.g., System instead of java.lang.System)
        Matcher simpleTypeMatcher = SIMPLE_TYPE_PATTERN.matcher(code);
        while (simpleTypeMatcher.find())
        {
            String simpleName = simpleTypeMatcher.group(1);
            int start = simpleTypeMatcher.start();
            int end = simpleTypeMatcher.end();

            // Check if this is actually a type reference, not a static field/method access
            boolean isTypeReference = false;

            if (start > 0)
            {
                char prevChar = code.charAt(start - 1);
                if (prevChar == '<' || prevChar == ',')
                {
                    // Type follows generic delimiter: <File>, Map<String, File>
                    isTypeReference = true;
                }
            }

            if (!isTypeReference && end < code.length())
            {
                char nextChar = code.charAt(end);
                if (nextChar == '.')
                {
                    // Check if followed by lowercase letter (static field/method access)
                    if (end + 1 < code.length())
                    {
                        char afterDot = code.charAt(end + 1);
                        if (Character.isLowerCase(afterDot) || Character.isDigit(afterDot))
                        {
                            // This is a static member access like System.out or System.exit
                            String fullName = simpleNameToFullName.get(simpleName);
                            if (fullName != null)
                            {
                                return fullName;
                            }
                        }
                    }
                }
                else if (nextChar == ';' || Character.isWhitespace(nextChar) || nextChar == '>' || nextChar == '['
                    || nextChar == '(' || nextChar == '=')
                {
                    // It's a type reference (declaration, cast, generic type, array, parameter, etc.)
                    isTypeReference = true;
                }
            }

            if (isTypeReference)
            {
                String fullName = simpleNameToFullName.get(simpleName);
                if (fullName != null)
                {
                    return fullName;
                }
            }
        }

		return null;
	}

	/**
	 * Checks if a type name is restricted.
	 *
	 * @param typeName The fully qualified class name to check
	 * @return true if the type is restricted, false otherwise
	 */
	private boolean isRestricted(String typeName)
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
            if (restricted.endsWith(".*")) //$NON-NLS-1$
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
