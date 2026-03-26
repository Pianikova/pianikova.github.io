package com.e1c.edt.ai.tools;

import java.util.Collections;

public class SimpleReplacer implements IReplacementStrategy
{
	@Override
	public Iterable<String> findCandidates(String content, String find)
	{
		return Collections.singletonList(find);
	}

	@Override
	public int getOrdinal()
	{
		return 0;
	}
}
