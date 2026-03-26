/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

/**
 * Description and usage example for a JShell binding.
 */
public class JShellBindingDescription
{
	private final String description;
	private final String example;
	private final Object value;
	private final Class<?> explicitType;

	/**
	 * Creates a binding description without example.
	 *
	 * @param description description of the binding
	 * @param value the binding value
	 * @param explicitType the explicit type of the binding (may differ from value.getClass())
	 */
	public JShellBindingDescription(String description, Object value, Class<?> explicitType)
	{
		this(description, null, value, explicitType);
	}

	/**
	 * Creates a binding description with example.
	 *
	 * @param description description of the binding
	 * @param example code example showing how to use the binding (can be null)
	 * @param value the binding value
	 * @param explicitType the explicit type of the binding (may differ from value.getClass())
	 */
	public JShellBindingDescription(String description, String example, Object value, Class<?> explicitType)
	{
		this.description = description;
		this.example = example;
		this.value = value;
		this.explicitType = explicitType;
	}

	/**
	 * Returns the description of this binding.
	 *
	 * @return binding description
	 */
	public String getDescription()
	{
		return description;
	}

	/**
	 * Returns the code example for this binding.
	 *
	 * @return code example or null if not provided
	 */
	public String getExample()
	{
		return example;
	}

	/**
	 * Returns the value of this binding.
	 *
	 * @return binding value
	 */
	public Object getValue()
	{
		return value;
	}

	/**
	 * Returns the explicit type of this binding.
	 *
	 * @return explicit binding type
	 */
	public Class<?> getExplicitType()
	{
		return explicitType;
	}
}
