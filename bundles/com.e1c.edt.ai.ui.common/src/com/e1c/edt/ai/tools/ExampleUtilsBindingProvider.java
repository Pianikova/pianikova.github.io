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
	public Map<String, JShellBindingDescription> getBindingInfos()
	{
		var infos = new HashMap<String, JShellBindingDescription>();
		infos.put("Math", new JShellBindingDescription("Java Math class with mathematical functions",
			"var result = Math.sqrt(25.0);\nvar pi = Math.PI;\nvar random = Math.random();"));
		infos.put("System", new JShellBindingDescription("Java System class for system utilities",
			"System.out.println(\"Hello from JShell\");\nvar properties = System.getProperties();\nSystem.setProperty(\"my.property\", \"value\");"));
		return infos;
	}
}
