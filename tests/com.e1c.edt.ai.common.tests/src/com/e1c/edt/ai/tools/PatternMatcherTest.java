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
		// A separator-less pattern matches the file name at any depth (gitignore-style),
		// so "test.bsl" now matches "src/test.bsl".
		assertTrue(matcher.matches("src/test.bsl", "test.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
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
        // Separator-less pattern matches the file name at any depth (gitignore-style).
        assertTrue(matcher.matches("src/MainModule.bsl", "*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(matcher.matches("src/MainModule.bsl", "*.java")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("src/module/test.bsl", "**/*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(matcher.matches("src/module/test.java", "**/*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("CommonModules/Module/Module.bsl", "**/Module.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(matcher.matches("CommonModules/Module/Module.java", "**/Module.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("src/Module.bsl", "src/**/*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("src/subdir/Module.bsl", "src/**/*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(matcher.matches("other/Module.bsl", "src/**/*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testCharacterClasses()
	{
		assertTrue(matcher.matches("file1.txt", "file[123].txt")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("file2.txt", "file[123].txt")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("file3.txt", "file[123].txt")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(matcher.matches("file4.txt", "file[123].txt")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("filea.txt", "file[a-z].txt")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("filez.txt", "file[a-z].txt")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(matcher.matches("fileA.txt", "file[a-z].txt")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("fileA.txt", "file[A-Z].txt")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("file0.txt", "file[0-9].txt")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("file9.txt", "file[0-9].txt")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(matcher.matches("filea.txt", "file[0-9].txt")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testBraceExpansion()
	{
		assertTrue(matcher.matches("file.txt", "file.{txt,md}")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("file.md", "file.{txt,md}")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(matcher.matches("file.java", "file.{txt,md}")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("test1.txt", "test{1,2,3}.txt")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("test2.txt", "test{1,2,3}.txt")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("test3.txt", "test{1,2,3}.txt")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(matcher.matches("test4.txt", "test{1,2,3}.txt")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("data.csv", "{file,data}.{csv,txt}")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("file.txt", "{file,data}.{csv,txt}")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(matcher.matches("other.txt", "{file,data}.{csv,txt}")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testCyrillicPatterns()
	{
		assertTrue(matcher.matches("файл.txt", "*.txt")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("Модуль.bsl", "*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("Справочник/Элемент.xml", "Справочник/*.xml")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(matcher.matches("Модуль.java", "*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("файл1.txt", "файл?.txt")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(matcher.matches("файл12.txt", "файл?.txt")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testDirectoryPrefixWithDoubleStar()
	{
		assertTrue(matcher.matches("CommonModules/Module/Module.bsl", "CommonModules/**/*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("CommonModules/Subdir/Module.bsl", "CommonModules/**/*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("CommonModules/Deep/Nested/Module.bsl", "CommonModules/**/*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(matcher.matches("src/Module.bsl", "CommonModules/**/*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(matcher.matches("Module.bsl", "CommonModules/**/*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("src/module/test.bsl", "src/**/test.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("src/test.bsl", "src/**/test.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(matcher.matches("src/module/other.bsl", "src/**/test.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testNestedRecursion()
	{
		assertTrue(matcher.matches("a/b/c/d/file.bsl", "a/**/c/**/file.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("a/c/file.bsl", "a/**/c/**/file.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("a/b/c/d/e/file.bsl", "a/**/c/**/file.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(matcher.matches("a/b/c/file.bsl", "a/**/d/**/file.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("src/module/submodule/test.bsl", "src/**/test.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testComplexCombination()
	{
		assertTrue(matcher.matches("Module1.bsl", "Module[0-9].bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("CommonModules/Module1/Module.bsl", "CommonModules/**/Module[0-9]/*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("file{test}.txt", "file{test}.txt")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("Module/test.bsl", "Module/{test,data}.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("Модуль1.bsl", "Модуль[0-9].bsl")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testDoubleStarWithDirectoryInMiddle()
	{
		// Test patterns like **/Temp/* should match files in Temp directory at any depth
		assertTrue(matcher.matches("Temp/test.bsl", "**/Temp/*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("src/Temp/test.bsl", "**/Temp/*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("src/deep/nested/Temp/Test1.bsl", "**/Temp/Test[1-2].bsl")); //$NON-NLS-1$ //$NON-NLS-2$

		// Negative tests - should not match files not in Temp directory
		assertFalse(matcher.matches("src/other/test.bsl", "**/Temp/*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(matcher.matches("Temp/test.bsl", "**/Other/*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testRealWorldIssuePatterns()
	{
		// Pattern: **/Temp/* should match files in Temp at any depth
		assertTrue(matcher.matches("src/Catalogs/Temp/Test1.bsl", "**/Temp/*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("src/Catalogs/Temp/test.bsl", "**/Temp/*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("Catalogs/Temp/data.bsl", "**/Temp/*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("Temp/file.txt", "**/Temp/*.txt")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("Other/Temp/file.bsl", "**/Temp/*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$

		// Pattern: **/Temp/* should NOT match files outside Temp
		assertFalse(matcher.matches("src/Catalogs/Other/Test1.bsl", "**/Temp/*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(matcher.matches("src/Catalogs/test.bsl", "**/Temp/*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(matcher.matches("src/Temp/test.bsl", "**/Other/*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$

		// Pattern: src/**/*.mdo should match .mdo files anywhere under src
		assertTrue(matcher.matches("src/Configuration/Configuration.mdo", "src/**/*.mdo")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("src/Catalogs/Temp/Test.mdo", "src/**/*.mdo")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("src/deep/nested/Test.mdo", "src/**/*.mdo")); //$NON-NLS-1$ //$NON-NLS-2$

		// Pattern: src/**/*.mdo should NOT match files outside src
		assertFalse(matcher.matches("Configuration/Configuration.mdo", "src/**/*.mdo")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(matcher.matches("test.mdo", "src/**/*.mdo")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testDirectoryPrefixPatterns()
	{
		// Test patterns like directory/**/* match files under directory at any depth
		assertTrue(matcher.matches("CommonModules/Module/Module.bsl", "CommonModules/**/*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("CommonModules/Subdir/Module.bsl", "CommonModules/**/*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("CommonModules/Deep/Nested/Module.bsl", "CommonModules/**/*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$

		// Should NOT match files outside CommonModules
		assertFalse(matcher.matches("src/Module.bsl", "CommonModules/**/*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(matcher.matches("Module.bsl", "CommonModules/**/*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(matcher.matches("other/CommonModules/Module.bsl", "CommonModules/**/*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testDoubleStarWithDirectoryPattern()
	{
		// More complex patterns: **/directory/**/*
		assertTrue(matcher.matches("src/Temp/module/file.bsl", "**/Temp/**/*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("Temp/module/file.bsl", "**/Temp/**/*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("Temp/file.bsl", "**/Temp/**/*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(matcher.matches("src/deep/Temp/nested/file.bsl", "**/Temp/**/*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$

		// Negative tests
		assertFalse(matcher.matches("src/other/file.bsl", "**/Temp/**/*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(matcher.matches("Temp/file.java", "**/Temp/**/*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
	}

    @Test
    public void testGlobRecursiveFilePatternWithWildcard()
    {
        // Test pattern: **/*Test*.java - matches files containing "Test" anywhere
        assertTrue(matcher.matches("PatternMatcherTest.java", "**/*Test*.java")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(matcher.matches("Test.java", "**/*Test*.java")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(matcher.matches("SomeTest.java", "**/*Test*.java")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(matcher.matches("MyTest.java", "**/*Test*.java")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(matcher.matches("directory/Test.java", "**/*Test*.java")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(matcher.matches("com/e1c/edt/ai/tools/PatternMatcherTest.java", "**/*Test*.java")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(matcher.matches("com/e1c/edt/ai/tools/SimpleTest.java", "**/*Test*.java")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(matcher.matches("com/e1c/edt/ai/tools/TreeBuilderTest.java", "**/*Test*.java")); //$NON-NLS-1$ //$NON-NLS-2$

        // Should NOT match files without "Test"
        assertFalse(matcher.matches("PatternMatcher.java", "**/*Test*.java")); //$NON-NLS-1$ //$NON-NLS-2$
        assertFalse(matcher.matches("Module.bsl", "**/*Test*.java")); //$NON-NLS-1$ //$NON-NLS-2$
        assertFalse(matcher.matches("Main.java", "**/*Test*.java")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    public void testGlobRecursiveSpecificFilePattern()
    {
        // Test pattern: **/*McpTool.java - matches MCP tool files anywhere
        assertTrue(matcher.matches("GlobMcpTool.java", "**/*McpTool.java")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(matcher.matches("tools/GlobMcpTool.java", "**/*McpTool.java")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(matcher.matches("com/e1c/edt/ai/tools/GlobMcpTool.java", "**/*McpTool.java")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(matcher.matches("JShellMcpTool.java", "**/*McpTool.java")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(matcher.matches("FindMcpTool.java", "**/*McpTool.java")); //$NON-NLS-1$ //$NON-NLS-2$

        // Should NOT match non-McpTool files
        assertFalse(matcher.matches("PatternMatcher.java", "**/*McpTool.java")); //$NON-NLS-1$ //$NON-NLS-2$
        assertFalse(matcher.matches("Test.java", "**/*McpTool.java")); //$NON-NLS-1$ //$NON-NLS-2$
        assertFalse(matcher.matches("GlobMcpTool.test", "**/*McpTool.java")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    public void testGlobRecursivePatternWithPrefixWildcard()
    {
        // Test pattern: **/PatternMatcher*.java - matches files starting with "PatternMatcher"
        assertTrue(matcher.matches("PatternMatcher.java", "**/PatternMatcher*.java")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(matcher.matches("PatternMatcherTest.java", "**/PatternMatcher*.java")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(matcher.matches("tools/PatternMatcher.java", "**/PatternMatcher*.java")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(matcher.matches("com/e1c/edt/ai/tools/PatternMatcher.java", "**/PatternMatcher*.java")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(matcher.matches("src/PatternMatcherOther.java", "**/PatternMatcher*.java")); //$NON-NLS-1$ //$NON-NLS-2$

        // Should NOT match files not starting with "PatternMatcher"
        assertFalse(matcher.matches("TestPatternMatcher.java", "**/PatternMatcher*.java")); //$NON-NLS-1$ //$NON-NLS-2$
        assertFalse(matcher.matches("MyPatternMatcher.java", "**/PatternMatcher*.java")); //$NON-NLS-1$ //$NON-NLS-2$
        assertFalse(matcher.matches("Other.java", "**/PatternMatcher*.java")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    public void testGlobRecursivePatternWithDeepNesting()
    {
        // Test deep nesting with ** patterns
        assertTrue(matcher.matches("a/b/c/d/e/Test.java", "**/Test.java")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(matcher.matches("very/deep/nested/path/to/Test.java", "**/Test.java")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(matcher.matches("com/e1c/edt/ai/tools/PatternMatcher.java", "**/PatternMatcher.java")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(matcher.matches("src/main/java/com/example/Main.java", "**/*.java")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(matcher.matches("a/b/c/d/e/f/g/h/i/j/Test.java", "**/*Test*.java")); //$NON-NLS-1$ //$NON-NLS-2$

        // Edge cases
        assertTrue(matcher.matches("Test.java", "**/Test.java")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(matcher.matches("a/Test.java", "**/Test.java")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    public void testCharacterClassNegation()
    {
        assertTrue(matcher.matches("filed.txt", "file[!abc].txt")); //$NON-NLS-1$ //$NON-NLS-2$
        assertFalse(matcher.matches("filea.txt", "file[!abc].txt")); //$NON-NLS-1$ //$NON-NLS-2$
        assertFalse(matcher.matches("fileb.txt", "file[!abc].txt")); //$NON-NLS-1$ //$NON-NLS-2$
        assertFalse(matcher.matches("filec.txt", "file[!abc].txt")); //$NON-NLS-1$ //$NON-NLS-2$

        assertTrue(matcher.matches("fileA.txt", "file[^0-9].txt")); //$NON-NLS-1$ //$NON-NLS-2$
        assertFalse(matcher.matches("file5.txt", "file[^0-9].txt")); //$NON-NLS-1$ //$NON-NLS-2$

        assertTrue(matcher.matches("fileX.bsl", "file[!a-z].bsl")); //$NON-NLS-1$ //$NON-NLS-2$
        assertFalse(matcher.matches("filex.bsl", "file[!a-z].bsl")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    public void testCyrillicCharacterClassRanges()
    {
        assertTrue(matcher.matches("файла.txt", "файл[а-я].txt")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(matcher.matches("файля.txt", "файл[а-я].txt")); //$NON-NLS-1$ //$NON-NLS-2$
        assertFalse(matcher.matches("файл1.txt", "файл[а-я].txt")); //$NON-NLS-1$ //$NON-NLS-2$

        assertTrue(matcher.matches("МодульА.bsl", "Модуль[А-Я].bsl")); //$NON-NLS-1$ //$NON-NLS-2$
        assertFalse(matcher.matches("Модульа.bsl", "Модуль[А-Я].bsl")); //$NON-NLS-1$ //$NON-NLS-2$

        // Negation with Cyrillic
        assertTrue(matcher.matches("файлX.txt", "файл[!а-я].txt")); //$NON-NLS-1$ //$NON-NLS-2$
        assertFalse(matcher.matches("файла.txt", "файл[!а-я].txt")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    public void testNestedBraceExpansion()
    {
        assertTrue(matcher.matches("file.txt", "file.{txt,{md,rst}}")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(matcher.matches("file.md", "file.{txt,{md,rst}}")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(matcher.matches("file.rst", "file.{txt,{md,rst}}")); //$NON-NLS-1$ //$NON-NLS-2$
        assertFalse(matcher.matches("file.java", "file.{txt,{md,rst}}")); //$NON-NLS-1$ //$NON-NLS-2$

        assertTrue(matcher.matches("src/main.bsl", "{src,test}/{main,helper}.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(matcher.matches("test/helper.bsl", "{src,test}/{main,helper}.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
        assertFalse(matcher.matches("src/other.bsl", "{src,test}/{main,helper}.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
        assertFalse(matcher.matches("other/main.bsl", "{src,test}/{main,helper}.bsl")); //$NON-NLS-1$ //$NON-NLS-2$

        // Triple nesting
        assertTrue(matcher.matches("a.log", "{a,{b,{c,d}}}.log")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(matcher.matches("c.log", "{a,{b,{c,d}}}.log")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(matcher.matches("d.log", "{a,{b,{c,d}}}.log")); //$NON-NLS-1$ //$NON-NLS-2$
        assertFalse(matcher.matches("e.log", "{a,{b,{c,d}}}.log")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    public void testNestedRecursionBacktracking()
    {
        assertTrue(matcher.matches("a/a/a/x.txt", "a/**/a/**/x.txt")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(matcher.matches("a/b/a/c/x.txt", "a/**/a/**/x.txt")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(matcher.matches("a/a/x.txt", "a/**/a/**/x.txt")); //$NON-NLS-1$ //$NON-NLS-2$
        assertFalse(matcher.matches("a/b/c/x.txt", "a/**/a/**/x.txt")); //$NON-NLS-1$ //$NON-NLS-2$

        // Pattern with repeating directory names requires correct backtracking
        assertTrue(matcher.matches("src/util/src/core/File.java", "src/**/src/**/File.java")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(matcher.matches("src/src/File.java", "src/**/src/**/File.java")); //$NON-NLS-1$ //$NON-NLS-2$
        assertFalse(matcher.matches("src/util/File.java", "src/**/src/**/File.java")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    public void testComplexBraceWithWildcards()
    {
        assertTrue(matcher.matches("Test1.java", "{Test*,Spec*}.java")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(matcher.matches("SpecRunner.java", "{Test*,Spec*}.java")); //$NON-NLS-1$ //$NON-NLS-2$
        assertFalse(matcher.matches("MainRunner.java", "{Test*,Spec*}.java")); //$NON-NLS-1$ //$NON-NLS-2$

        // Brace containing ? wildcard
        assertTrue(matcher.matches("fileA.txt", "file{?,*}.txt")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(matcher.matches("fileABC.txt", "file{?,*}.txt")); //$NON-NLS-1$ //$NON-NLS-2$

        // Brace combined with character class
        assertTrue(matcher.matches("Module1.bsl", "{Module,Catalog}[0-9].bsl")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(matcher.matches("Catalog5.bsl", "{Module,Catalog}[0-9].bsl")); //$NON-NLS-1$ //$NON-NLS-2$
        assertFalse(matcher.matches("Other5.bsl", "{Module,Catalog}[0-9].bsl")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    public void testSeparatorlessPatternMatchesNestedFiles()
    {
        // Reproduces the reported bug: a separator-less pattern like "*Module*" must find files
        // in nested folders, not only at the root (real 1C/EDT layout: src/<kind>/<name>/<file>).
        assertTrue(matcher.matches("ManagerModule.bsl", "*Module*")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(matcher.matches("CommonModules/ОбщегоНазначения/Module.bsl", "*Module*")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(matcher.matches("src/Catalogs/Контрагенты/ManagerModule.bsl", "*Module*")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(matcher.matches("src\\Documents\\ПоступлениеТоваров\\ObjectModule.bsl", "*Module*")); //$NON-NLS-1$ //$NON-NLS-2$

        // The file name still has to contain the pattern.
        assertFalse(matcher.matches("src/Catalogs/Контрагенты/Контрагенты.mdo", "*Module*")); //$NON-NLS-1$ //$NON-NLS-2$
        // Only the file name is matched, not the folders, so "Catalogs" in the path is ignored.
        assertFalse(matcher.matches("src/Catalogs/Контрагенты/Module.bsl", "*Catalogs*")); //$NON-NLS-1$ //$NON-NLS-2$

        // Other separator-less patterns also match at any depth.
        assertTrue(matcher.matches("src/CommonModules/Служебный/Module.bsl", "*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(matcher.matches("src/Catalogs/Контрагенты/Контрагенты.mdo", "*.mdo")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(matcher.matches("src/Catalogs/Контрагенты/Контрагенты.mdo", "Контрагенты.*")); //$NON-NLS-1$ //$NON-NLS-2$

        // A pattern WITH a separator stays anchored - it does not match deeper paths.
        assertFalse(matcher.matches("src/CommonModules/Служебный/Module.bsl", "src/*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(matcher.matches("src/Module.bsl", "src/*.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    public void testSingleCharWildcardWithUnicode()
    {
        assertTrue(matcher.matches("фаил.txt", "фа?л.txt")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(matcher.matches("файл.txt", "фа?л.txt")); //$NON-NLS-1$ //$NON-NLS-2$
        assertFalse(matcher.matches("фа.txt", "фа?л.txt")); //$NON-NLS-1$ //$NON-NLS-2$

        assertTrue(matcher.matches("Модуль.bsl", "?одуль.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(matcher.matches("Bодуль.bsl", "?одуль.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
        assertFalse(matcher.matches("одуль.bsl", "?одуль.bsl")); //$NON-NLS-1$ //$NON-NLS-2$
    }

}
