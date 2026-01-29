/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai;

/**
 * Enum for text colors used in styled text
 */
public enum TextColor
{
	BLACK("black"), //$NON-NLS-1$
	WHITE("white"), //$NON-NLS-1$
	RED("red"), //$NON-NLS-1$
	GREEN("green"), //$NON-NLS-1$
	BLUE("blue"), //$NON-NLS-1$
	YELLOW("yellow"), //$NON-NLS-1$
	ORANGE("orange"), //$NON-NLS-1$
	PURPLE("purple"), //$NON-NLS-1$
	PINK("pink"), //$NON-NLS-1$
	GRAY("gray"), //$NON-NLS-1$
	CYAN("cyan"), //$NON-NLS-1$
	MAGENTA("magenta"); //$NON-NLS-1$

	private final String value;

	TextColor(String value)
	{
		this.value = value;
	}

	public String getValue()
	{
		return value;
	}
}