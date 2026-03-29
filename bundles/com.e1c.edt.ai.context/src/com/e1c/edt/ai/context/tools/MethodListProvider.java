/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context.tools;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.google.inject.Singleton;

/**
 * Default implementation of {@link IMethodListProvider} using reflection.
 */
@Singleton
public class MethodListProvider
    implements IMethodListProvider
{
	@Override
	@SuppressWarnings("nls")
	public List<String> getPublicMethodSignatures(Class<?> clazz)
	{
		var methods = clazz.getMethods();
		return Arrays.stream(methods)
			.filter(method -> method.getName().startsWith("create") || method.getName().startsWith("get"))
			.map(this::buildMethodSignature)
			.distinct()
			.sorted()
			.collect(Collectors.toList());
	}

	@SuppressWarnings("nls")
	private String buildMethodSignature(Method method)
	{
		var signature = new StringBuilder();
		signature.append(method.getName()).append("(");

		var parameters = method.getParameters();
		for (int i = 0; i < parameters.length; i++)
		{
			if (i > 0)
			{
				signature.append(", ");
			}
			Parameter param = parameters[i];
			String typeName = param.getType().getSimpleName();
			signature.append(typeName);
		}

		signature.append(")");
		return signature.toString();
	}
}
