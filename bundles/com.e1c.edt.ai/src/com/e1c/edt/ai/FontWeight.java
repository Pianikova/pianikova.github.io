/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai;

/**
 * Enum for font weights used in styled text
 */
public enum FontWeight
{
	NORMAL("normal"), //$NON-NLS-1$
	BOLD("bold"), //$NON-NLS-1$
	BOLDER("bolder"), //$NON-NLS-1$
	LIGHTER("lighter"), //$NON-NLS-1$
	THIN("100"), //$NON-NLS-1$
	EXTRA_LIGHT("200"), //$NON-NLS-1$
	LIGHT("300"), //$NON-NLS-1$
	MEDIUM("500"), //$NON-NLS-1$
	SEMI_BOLD("600"), //$NON-NLS-1$
	BOLD_VALUE("700"), //$NON-NLS-1$
	EXTRA_BOLD("800"), //$NON-NLS-1$
	BLACK("900"); //$NON-NLS-1$

	private final String value;

	FontWeight(String value)
	{
		this.value = value;
	}

	public String getValue()
	{
		return value;
	}
}