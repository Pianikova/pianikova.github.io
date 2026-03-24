package com.e1c.edt.ai.tools;

public interface IReplacementStrategy
{
	/**
	 * Finds replacement candidates in content.
	 *
	 * @param content file content normalized to \n
	 * @param find requested old content normalized to \n
	 * @return candidates to be searched and replaced
	 */
	Iterable<String> findCandidates(String content, String find);

	/**
	 * Gets the ordinal number of the strategy.
	 *
	 * @return the ordinal number of the strategy
	 */
	int getOrdinal();
}
