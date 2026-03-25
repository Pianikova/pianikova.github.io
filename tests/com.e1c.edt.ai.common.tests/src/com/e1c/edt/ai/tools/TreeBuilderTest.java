/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.e1c.edt.ai.IMarkdownUtils;

/**
 * Tests for TreeBuilder class.
 */
@SuppressWarnings("nls")
public class TreeBuilderTest
{
	private IMarkdownUtils markdownUtils;
	private TreeBuilder treeBuilder;

	@Before
	public void setUp()
	{
		markdownUtils = Mockito.mock(IMarkdownUtils.class);
		Mockito.when(markdownUtils.escapeForMarkdown(Mockito.anyString())).thenAnswer(invocation -> invocation.getArgument(0));
		treeBuilder = new TreeBuilder(markdownUtils);
	}

	@Test
	public void testBuildReturnsEmptyStringWhenNothingAdded()
	{
		String result = treeBuilder.build();
		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

	@Test
	public void testAddSingleRootDirectory()
	{
		treeBuilder.addDirectory("root", 0);
		String result = treeBuilder.build();

		assertEquals("root/", result);
	}

	@Test
	public void testAddSingleRootFile()
	{
		treeBuilder.addFile("file.txt", 0);
		String result = treeBuilder.build();

		assertEquals("\n  ├── file.txt", result);
	}

	@Test
	public void testAddSingleNestedDirectory()
	{
		treeBuilder.addDirectory("parent", 0);
		treeBuilder.addDirectory("child", 1);
		String result = treeBuilder.build();

        assertEquals("parent/\n │   └── child/", result);
	}

	@Test
	public void testAddNestedFile()
	{
		treeBuilder.addDirectory("parent", 0);
		treeBuilder.addFile("file.txt", 1);
		String result = treeBuilder.build();

        assertEquals("parent/\n │    ├── file.txt", result);
	}

	@Test
	public void testMultipleSiblingsAtSameDepth()
	{
		treeBuilder.addDirectory("parent", 0);
		treeBuilder.addDirectory("sibling1", 1);
		treeBuilder.addDirectory("sibling2", 1);
		String result = treeBuilder.build();

        assertEquals("parent/\n" + " │   └── sibling1/\n" + " ├── sibling2/", result);
	}

	@Test
	public void testMultipleFilesAtSameDepth()
	{
		treeBuilder.addDirectory("parent", 0);
		treeBuilder.addFile("file1.txt", 1);
		treeBuilder.addFile("file2.txt", 1);
		String result = treeBuilder.build();

        assertEquals("parent/\n │    ├── file1.txt\n │    ├── file2.txt", result);
	}

	@Test
	public void testComplexNestedStructure()
	{
		treeBuilder.addDirectory("root", 0);
		treeBuilder.addDirectory("dir1", 1);
		treeBuilder.addDirectory("subdir1", 2);
		treeBuilder.addFile("file1.txt", 3);
		treeBuilder.addFile("file2.txt", 2);
		treeBuilder.addDirectory("dir2", 1);
		treeBuilder.addFile("file3.txt", 2);
		String result = treeBuilder.build();

		assertEquals("root/\n"
            + " │   └── dir1/\n" + "     │   └── subdir1/\n" + "         │    ├── file1.txt\n"
            + "         └── file2.txt\n" + " └── dir2/\n" + "     │   └── file3.txt", result);
	}

	@Test
	public void testEndDirectoryDoesNothing()
	{
		treeBuilder.addDirectory("dir1", 0);
		treeBuilder.addDirectory("dir2", 1);
		treeBuilder.endDirectory();
		treeBuilder.addFile("file.txt", 1);
		String result = treeBuilder.build();

        assertEquals("dir1/\n" + " │   └── dir2/\n" + "     └── file.txt", result);
	}

	@Test
	public void testMultipleRootDirectories()
	{
		treeBuilder.addDirectory("root1", 0);
		treeBuilder.addDirectory("root2", 0);
		String result = treeBuilder.build();

        assertEquals("root1/root2/", result);
	}

	@Test
	public void testDeeplyNestedDirectories()
	{
		treeBuilder.addDirectory("level0", 0);
		treeBuilder.addDirectory("level1", 1);
		treeBuilder.addDirectory("level2", 2);
		treeBuilder.addDirectory("level3", 3);
		treeBuilder.addDirectory("level4", 4);
		String result = treeBuilder.build();

		assertEquals("level0/\n"
            + " │   └── level1/\n" + "     │   └── level2/\n" + "         │   └── level3/\n"
            + "             │   └── level4/", result);
	}

	@Test
	public void testMultipleChildrenWithGrandchildren()
	{
		treeBuilder.addDirectory("root", 0);
		treeBuilder.addDirectory("child1", 1);
		treeBuilder.addDirectory("grandchild1", 2);
		treeBuilder.addDirectory("grandchild2", 2);
		treeBuilder.addDirectory("child2", 1);
		treeBuilder.addDirectory("grandchild3", 2);
		String result = treeBuilder.build();

		assertEquals("root/\n"
            + " │   └── child1/\n" + "     │   └── grandchild1/\n" + "     ├── grandchild2/\n" + " └── child2/\n"
            + "     ├── grandchild3/", result);
	}

	@Test
	public void testMixedFilesAndDirectoriesAtDifferentDepths()
	{
		treeBuilder.addDirectory("root", 0);
		treeBuilder.addFile("rootfile.txt", 1);
		treeBuilder.addDirectory("subdir", 1);
		treeBuilder.addFile("subfile.txt", 2);
		String result = treeBuilder.build();

		assertEquals("root/\n"
            + " │    ├── rootfile.txt\n" + " │   └── subdir/\n" + "     │    ├── subfile.txt", result);
	}

	@Test
	public void testBuildCanBeCalledMultipleTimes()
	{
		treeBuilder.addDirectory("dir1", 0);
		String result1 = treeBuilder.build();
		String result2 = treeBuilder.build();

		assertEquals(result1, result2);
		assertEquals("dir1/", result1);
	}

	@Test
	public void testBuilderAfterMultipleBuilds()
	{
		treeBuilder.addDirectory("dir1", 0);
		treeBuilder.build();
		treeBuilder.addDirectory("dir2", 0);
		String result = treeBuilder.build();

        assertEquals("dir1/dir2/", result);
	}

	@Test
	public void testSpecialCharactersInNames()
	{
		treeBuilder.addDirectory("dir with spaces", 0);
		treeBuilder.addFile("file-with-dashes.txt", 1);
		String result = treeBuilder.build();

		assertEquals("dir with spaces/\n"
            + " │    ├── file-with-dashes.txt", result);
	}

	@Test
	public void testMarkdownEscapingIsApplied()
	{
		Mockito.when(markdownUtils.escapeForMarkdown("dir`name")).thenReturn("dir\\`name");
		Mockito.when(markdownUtils.escapeForMarkdown("file.txt")).thenReturn("file.txt");

		treeBuilder.addDirectory("dir`name", 0);
		treeBuilder.addFile("file.txt", 1);
		String result = treeBuilder.build();

        assertEquals("dir\\`name/\n │    ├── file.txt", result);
	}

	@Test
	public void testLargeDirectoryTree()
	{
		treeBuilder.addDirectory("root", 0);
		for (int i = 1; i <= 5; i++)
		{
			treeBuilder.addDirectory("dir" + i, 1);
		}
		String result = treeBuilder.build();

		assertEquals("root/\n"
            + " │   └── dir1/\n" + " ├── dir2/\n" + " ├── dir3/\n" + " ├── dir4/\n" + " ├── dir5/", result);
    }

    @Test
    public void testSingleFileAtRoot()
    {
        treeBuilder.addFile("file.txt", 0);
        String result = treeBuilder.build();

        assertEquals("\n  ├── file.txt", result);
	}

    @Test
    public void testEmptyDirectoryName()
    {
        treeBuilder.addDirectory("", 0);
        String result = treeBuilder.build();

        assertEquals("/", result);
    }

    @Test
    public void testEmptyFileName()
    {
        treeBuilder.addDirectory("dir", 0);
        treeBuilder.addFile("", 1);
        String result = treeBuilder.build();

        assertEquals("dir/\n │    ├── ", result);
    }
}