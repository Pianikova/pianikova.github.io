package com.e1c.edt.ai.tools;

import java.util.ArrayList;
import java.util.List;

public class MultiOccurrenceReplacer implements IReplacementStrategy
{
	@Override
	public Iterable<String> findCandidates(String content, String find)
	{
		List<String> matches = new ArrayList<>();
		if (find.isEmpty())
		{
			return matches;
		}

		int startIndex = 0;
		while (true)
		{
			int index = content.indexOf(find, startIndex);
			if (index == -1)
			{
				break;
			}

			matches.add(find);
			startIndex = index + find.length();
		}

		return matches;
	}

	@Override
	public int getOrdinal()
	{
		return 8;
	}
}
