/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

/**
* Interface for building directory tree representations
*/
public interface ITreeBuilder
{

	/**
	 * Add a directory to the tree at the specified depth
	 * @param name directory name or relative path
	 * @param depth depth in the tree structure
	 */
	void addDirectory(String name, int depth);

	/**
	 * Add a file to the tree at the specified depth
	 * @param name file name or relative path
	 * @param depth depth in the tree structure
	 */
	void addFile(String name, int depth);

	/**
	 * End the current directory context
	 */
	void endDirectory();

	/**
	 * Build and return the complete tree representation
	 * @return tree as a string
	 */
	String build();
}
