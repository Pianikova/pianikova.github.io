/**
*
*/
package com.e1c.edt.ai.tools;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests for GlobPatternMatcher.
 */
public class PatternMatcherTest
{

	private IPatternMatcher matcher;

	@Before
	public void setUp()
	{
		matcher = new PatternMatcher();
	}

	@Test
	public void testSimplePatternMatches()
	{
		assertTrue(matcher.matches("test.bsl", "*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("Module.bsl", "*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(matcher.matches("test.java", "*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testQuestionMarkPatternMatches()
	{
		assertTrue(matcher.matches("test.bsl", "test.???")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(matcher.matches("file.txt", "????????")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(matcher.matches("filename.txt", "????")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testStarInSegmentMatches()
	{
		assertTrue(matcher.matches("test_file.bsl", "test_*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("myfile.txt", "*.txt")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("data.xml", "*.xml")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(matcher.matches("data.xml", "*.json")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testDoubleStarMatchesMultipleSegments()
	{
		assertTrue(matcher.matches("src/module/file.bsl", "**/*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("file.bsl", "**/*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("deep/nested/path/file.bsl", "**/*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(matcher.matches("src/module/file.java", "**/*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testDoubleStarAtBeginning()
	{
		assertTrue(matcher.matches("test.bsl", "**/test.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("src/test.bsl", "**/test.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("src/deep/test.bsl", "**/test.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(matcher.matches("test.java", "**/test.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(matcher.matches("src/other.bsl", "**/test.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testDoubleStarAtEnd()
	{
		assertTrue(matcher.matches("src/module/file.bsl", "src/**")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("src/file.bsl", "src/**")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("src/very/deep/nested/path.bsl", "src/**")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(matcher.matches("other/file.bsl", "src/**")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testDoubleStarInMiddle()
	{
		assertTrue(matcher.matches("src/module/file.bsl", "src/**/file.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("src/deep/nested/file.bsl", "src/**/file.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("src/file.bsl", "src/**/file.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(matcher.matches("src/module/other.bsl", "src/**/file.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(matcher.matches("other/module/file.bsl", "src/**/file.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testMultipleSegmentsPattern()
	{
		assertTrue(matcher.matches("src/module/file.bsl", "src/module/*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("src/module/test.bsl", "src/module/test.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(matcher.matches("src/other/file.bsl", "src/module/*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(matcher.matches("src/module/file.java", "src/module/*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testPathWithBackslash()
	{
		assertTrue(matcher.matches("src\\module\\file.bsl", "src/module/*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("dir\\subdir\\test.java", "**/*.java")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(matcher.matches("src\\module\\file.java", "src/module/*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testPatternWithBackslash()
	{
		assertTrue(matcher.matches("src/module/file.bsl", "src\\module\\*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("dir/subdir/test.java", "dir\\**\\*.java")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(matcher.matches("src/module/file.java", "src\\module\\*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testExactMatch()
	{
		assertTrue(matcher.matches("file.bsl", "file.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("src/module/test.bsl", "src/module/test.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(matcher.matches("file.java", "file.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(matcher.matches("src/module/test2.bsl", "src/module/test.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testComplexPattern()
	{
		assertTrue(matcher.matches("src/module/TestFile.bsl", "src/**/Test*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("src/module/SubModule/TestFile.bsl", "src/**/Test*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(matcher.matches("src/module/OtherFile.bsl", "src/**/Test*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(matcher.matches("src/module/TestFile.java", "src/**/Test*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testDotEscaping()
	{
		assertTrue(matcher.matches("file.bsl", "*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(matcher.matches("fileXbsl", "*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("data.xml", "*.xml")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(matcher.matches("dataYxml", "*.xml")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testMixedWildcards()
	{
		assertTrue(matcher.matches("test123.txt", "test???.txt")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("file.bsl", "f???.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(matcher.matches("file123.txt", "test???.txt")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("src/Module1.bsl", "src/Module*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("src/Module99.bsl", "src/Module*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(matcher.matches("src/ModuleTest.bsl", "src/Module?.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testDoubleStarWithMultipleSegments()
	{
		assertTrue(matcher.matches("a/b/c/d/file.bsl", "a/**/d/**/file.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("a/d/file.bsl", "a/**/d/**/file.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("a/b/c/d/e/file.bsl", "a/**/d/**/file.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(matcher.matches("a/b/c/file.bsl", "a/**/d/**/file.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(matcher.matches("a/b/c/d/file.java", "a/**/d/**/file.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testDoubleStarMatchesEmptySegments()
	{
		assertTrue(matcher.matches("file.bsl", "**/file.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("src/file.bsl", "**/file.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("src/dir/file.bsl", "**/file.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testPatternDoesNotMatchLongerPath()
	{
		assertFalse(matcher.matches("test.bsl", "test")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(matcher.matches("src/test.bsl", "test.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testLongerPatternDoesNotMatchShorterPath()
	{
		assertFalse(matcher.matches("test.bsl", "src/test.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(matcher.matches("file.bsl", "src/module/file.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testTrailingDoubleStar()
	{
		assertTrue(matcher.matches("file.bsl", "file.bsl/**")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("file.bsl", "file.bsl/**")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("src/dir", "src/**")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testConsecutiveStarsInSegment()
	{
		assertTrue(matcher.matches("abc.txt", "**.txt")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("abc.txt", "a**.txt")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(matcher.matches("abc.txt", "a**.java")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testMultipleDoubleStars()
	{
		assertTrue(matcher.matches("a/b/c/d/e/f.txt", "a/**/c/**/e/**/*.txt")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("a/c/e/f.txt", "a/**/c/**/e/**/*.txt")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(matcher.matches("a/b/c/e/f.txt", "a/**/c/**/e/**/*.txt")); //$NON-NLS-1$ //$NON-NLS-2$
        assertFalse(matcher.matches("a/b/d/e/f.txt", "a/**/c/**/e/**/*.txt")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testSegmentWithQuestionMarks()
	{
		assertTrue(matcher.matches("file1.txt", "file?.txt")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("fileA.txt", "file?.txt")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(matcher.matches("file12.txt", "file?.txt")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("src/File1.bsl", "src/File?.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(matcher.matches("src/File12.bsl", "src/File?.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testEmptyPattern()
	{
		assertFalse(matcher.matches("file.bsl", "")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(matcher.matches("src/file.bsl", "")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(matcher.matches("", "file.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testEmptyPathWithDoubleStar()
	{
		assertTrue(matcher.matches("", "**")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("", "/**")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testRealWorldPatterns()
	{
        assertFalse(matcher.matches("src/MainModule.bsl", "*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(matcher.matches("src/MainModule.bsl", "*.java")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("src/module/test.bsl", "**/*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(matcher.matches("src/module/test.java", "**/*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("CommonModules/Module/Module.bsl", "**/Module.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(matcher.matches("CommonModules/Module/Module.java", "**/Module.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("src/Module.bsl", "src/**/*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("src/subdir/Module.bsl", "src/**/*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(matcher.matches("other/Module.bsl", "src/**/*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
	}
}
