/**
 * Copyright (C) 2026, 1C
 */
package com.e1c.edt.ai.tools;

/**
 * Snapshot of the current state of a file: the live editor buffer when it carries unsaved
 * changes, otherwise the on-disk content.
 */
class CurrentFileContent
{
	public final byte[] content;
	public final boolean isDirty;

	CurrentFileContent(byte[] content, boolean isDirty)
	{
		this.content = content;
		this.isDirty = isDirty;
	}
}
