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

	/**
	 * Creates a binding description without example.
	 *
	 * @param description description of the binding
	 */
	public JShellBindingDescription(String description)
	{
		this(description, null);
	}

	/**
	 * Creates a binding description with example.
	 *
	 * @param description description of the binding
	 * @param example code example showing how to use the binding (can be null)
	 */
	public JShellBindingDescription(String description, String example)
	{
		this.description = description;
		this.example = example;
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
}
