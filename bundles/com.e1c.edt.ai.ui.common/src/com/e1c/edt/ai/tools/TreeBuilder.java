/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.util.ArrayList;
import java.util.List;

import com.e1c.edt.ai.IMarkdownUtils;
import com.google.inject.Inject;

/**
* Implementation of tree builder for directory structure representation
*/
public class TreeBuilder
	implements ITreeBuilder
{

	private final IMarkdownUtils markdownUtils;
	private final StringBuilder tree = new StringBuilder();
	private final List<Integer> stack = new ArrayList<>();

	@Inject
	public TreeBuilder(IMarkdownUtils markdownUtils)
	{
		this.markdownUtils = markdownUtils;
	}

	@SuppressWarnings("nls")
	@Override
	public void addDirectory(String name, int depth)
	{
		if (depth > 0)
		{
			tree.append("\n");
		}

		for (int i = 0; i < depth; i++)
		{
			if (i < stack.size() && stack.get(i) > 0)
			{
				tree.append(" │  ");
			}
			else if (i < depth - 1)
			{
				tree.append("    ");
			}
		}

		if (depth > 0 && stack.size() > depth - 1)
		{
			stack.set(depth - 1, stack.get(depth - 1) - 1);
		}

		if (depth > 0)
		{
			boolean hasMore = depth < stack.size() && stack.get(depth) > 0;
			tree.append(hasMore ? " ├── " : " └── ");
		}

		tree.append(markdownUtils.escapeForMarkdown(name)).append("/");

		while (stack.size() <= depth)
		{
			stack.add(0);
		}
		stack.set(depth, stack.get(depth) + 1);
	}

	@SuppressWarnings("nls")
	@Override
	public void addFile(String name, int depth)
	{
		tree.append("\n");

		for (int i = 0; i < depth; i++)
		{
			if (i < stack.size() && stack.get(i) > 0)
			{
				tree.append(" │  ");
			}
			else
			{
				tree.append("    ");
			}
		}

		if (stack.size() > depth)
		{
			stack.set(depth, stack.get(depth) - 1);
			boolean hasMore = stack.get(depth) > 0;
			tree.append(hasMore ? " ├── " : " └── ");
		}
		else
		{
			tree.append("  ├── ");
		}

		tree.append(markdownUtils.escapeForMarkdown(name));
	}

	@Override
	public void endDirectory()
	{
		//
	}

	@Override
	public String build()
	{
		return tree.toString();
	}
}
