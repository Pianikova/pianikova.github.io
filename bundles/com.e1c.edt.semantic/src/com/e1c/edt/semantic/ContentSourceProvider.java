/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.semantic;

import java.util.Optional;

import org.eclipse.core.resources.IFile;

import com.e1c.edt.ai.IContentSourceProvider;
import com.e1c.edt.ai.IFileDocument;

public class ContentSourceProvider
	implements IContentSourceProvider
{
	@Override
	public Optional<IFileDocument> getFileDocument(IFile file)
	{
		return Optional.empty();
	}
}
