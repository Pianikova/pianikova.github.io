/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.util.HashMap;
import java.util.Map;

import com.google.inject.Singleton;

/**
 * Example binding provider that adds utility objects to JShell.
 */
@Singleton
public class ExampleUtilsBindingProvider
	implements IJShellBindingProvider
{
    @SuppressWarnings("nls")
    @Override
	public Map<String, Object> getBindings()
	{
		var bindings = new HashMap<String, Object>();
		bindings.put("Math", Math.class);
		bindings.put("System", System.class);
		return bindings;
	}

    @SuppressWarnings("nls")
    @Override
	public Map<String, String> getBindingDescriptions()
	{
		var descriptions = new HashMap<String, String>();
		descriptions.put("Math", "Java Math class with mathematical functions");
		descriptions.put("System", "Java System class for system utilities");
		return descriptions;
	}
}
