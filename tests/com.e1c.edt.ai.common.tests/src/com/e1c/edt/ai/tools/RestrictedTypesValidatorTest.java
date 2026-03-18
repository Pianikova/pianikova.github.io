/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.e1c.edt.ai.ToolException;

/**
 * Tests for RestrictedTypesValidator.
 */
public class RestrictedTypesValidatorTest
{
	private RestrictedTypesValidator validator;
	private Set<String> restrictedTypes;

	@Before
	public void setUp()
	{
		restrictedTypes = new HashSet<>();
        restrictedTypes.add("java.io.File"); //$NON-NLS-1$
        restrictedTypes.add("java.io.FileInputStream"); //$NON-NLS-1$
        restrictedTypes.add("java.io.FileOutputStream"); //$NON-NLS-1$
        restrictedTypes.add("java.io.FileReader"); //$NON-NLS-1$
        restrictedTypes.add("java.io.FileWriter"); //$NON-NLS-1$
        restrictedTypes.add("java.io.RandomAccessFile"); //$NON-NLS-1$
        restrictedTypes.add("java.lang.System"); //$NON-NLS-1$
        restrictedTypes.add("java.lang.Runtime"); //$NON-NLS-1$
        restrictedTypes.add("java.lang.ProcessBuilder"); //$NON-NLS-1$
        restrictedTypes.add("java.lang.Process"); //$NON-NLS-1$
        restrictedTypes.add("java.lang.reflect.*"); //$NON-NLS-1$
        restrictedTypes.add("sun.reflect.*"); //$NON-NLS-1$
        restrictedTypes.add("java.lang.ClassLoader"); //$NON-NLS-1$
        restrictedTypes.add("java.net.URLClassLoader"); //$NON-NLS-1$
        restrictedTypes.add("java.net.Socket"); //$NON-NLS-1$
        restrictedTypes.add("java.net.ServerSocket"); //$NON-NLS-1$
        restrictedTypes.add("java.net.HttpURLConnection"); //$NON-NLS-1$
        restrictedTypes.add("java.net.URL"); //$NON-NLS-1$
        restrictedTypes.add("java.net.URI"); //$NON-NLS-1$
        restrictedTypes.add("java.nio.file.*"); //$NON-NLS-1$
        restrictedTypes.add("java.nio.channels.FileChannel"); //$NON-NLS-1$

        IRestrictedTypesProvider provider = new TestRestrictedTypesProvider(restrictedTypes);
        validator = new RestrictedTypesValidator(provider);
	}

	@Test
	public void testEmptyCode() throws ToolException
	{
		validator.validate(""); //$NON-NLS-1$
		validator.validate("   \n\t  "); //$NON-NLS-1$
	}

	@Test
	public void testNullCode() throws ToolException
	{
		validator.validate(null);
	}

	@Test
	public void testAllowedTypeImport() throws ToolException
	{
		String code = "import java.util.List;\nList<String> list = new ArrayList<>();"; //$NON-NLS-1$
		validator.validate(code);
	}

	@Test
	public void testAllowedTypeCreation() throws ToolException
	{
		String code = "String str = new String(\"test\");"; //$NON-NLS-1$
		validator.validate(code);
	}

	@Test
	public void testRestrictedTypeImport()
	{
		String code = "import java.io.File;\nFile f = new File(\"test.txt\");"; //$NON-NLS-1$
		try
		{
			validator.validate(code);
			fail("ToolException should be thrown for restricted type"); //$NON-NLS-1$
		}
		catch (ToolException e)
		{
			assertTrue(e.getMessage().contains("java.io.File")); //$NON-NLS-1$
		}
	}

	@Test
	public void testRestrictedTypeCreation()
	{
		String code = "java.io.File f = new java.io.File(\"test.txt\");"; //$NON-NLS-1$
		try
		{
			validator.validate(code);
			fail("ToolException should be thrown for restricted type"); //$NON-NLS-1$
		}
		catch (ToolException e)
		{
			assertTrue(e.getMessage().contains("java.io.File")); //$NON-NLS-1$
		}
	}

	@Test
	public void testRestrictedTypeCast()
	{
		String code = "Object obj = \"test\";\njava.io.File f = (java.io.File) obj;"; //$NON-NLS-1$
		try
		{
			validator.validate(code);
			fail("ToolException should be thrown for restricted type"); //$NON-NLS-1$
		}
		catch (ToolException e)
		{
			assertTrue(e.getMessage().contains("java.io.File")); //$NON-NLS-1$
		}
	}

	@Test
	public void testRestrictedPackageWildcard()
	{
		String code = "import java.nio.file.Path;\nPath p = Path.of(\"/tmp\");"; //$NON-NLS-1$
		try
		{
			validator.validate(code);
			fail("ToolException should be thrown for restricted package"); //$NON-NLS-1$
		}
		catch (ToolException e)
		{
			assertTrue(e.getMessage().contains("java.nio.file")); //$NON-NLS-1$
		}
	}

	@Test
	public void testRestrictedPackageFullQualifiedName()
	{
		String code = "java.nio.file.Files.createFile(java.nio.file.Path.of(\"/tmp/test\"));"; //$NON-NLS-1$
		try
		{
			validator.validate(code);
			fail("ToolException should be thrown for restricted package"); //$NON-NLS-1$
		}
		catch (ToolException e)
		{
			assertTrue(e.getMessage().contains("java.nio.file")); //$NON-NLS-1$
		}
	}

	@Test
	public void testRestrictedReflectionPackage()
	{
		String code = "import java.lang.reflect.Field;"; //$NON-NLS-1$
		try
		{
			validator.validate(code);
			fail("ToolException should be thrown for restricted reflection package"); //$NON-NLS-1$
		}
		catch (ToolException e)
		{
			assertTrue(e.getMessage().contains("java.lang.reflect")); //$NON-NLS-1$
		}
	}

	@Test
	public void testSystemExit()
	{
		String code = "System.exit(0);"; //$NON-NLS-1$
		try
		{
			validator.validate(code);
			fail("ToolException should be thrown for System class"); //$NON-NLS-1$
		}
		catch (ToolException e)
		{
			assertTrue(e.getMessage().contains("java.lang.System")); //$NON-NLS-1$
		}
	}

	@Test
	public void testFileInputStream()
	{
		String code = "new java.io.FileInputStream(\"test.txt\");"; //$NON-NLS-1$
		try
		{
			validator.validate(code);
			fail("ToolException should be thrown for FileInputStream"); //$NON-NLS-1$
		}
		catch (ToolException e)
		{
			assertTrue(e.getMessage().contains("java.io.FileInputStream")); //$NON-NLS-1$
		}
	}

	@Test
	public void testMultipleImportsWithRestricted()
	{
		String code = "import java.util.List;\nimport java.io.File;\nimport java.util.Map;"; //$NON-NLS-1$
		try
		{
			validator.validate(code);
			fail("ToolException should be thrown for restricted type in imports"); //$NON-NLS-1$
		}
		catch (ToolException e)
		{
			assertTrue(e.getMessage().contains("java.io.File")); //$NON-NLS-1$
		}
	}

	@Test
	public void testStaticImport()
	{
		String code = "import static java.lang.System.exit;"; //$NON-NLS-1$
		try
		{
			validator.validate(code);
			fail("ToolException should be thrown for static import of restricted type"); //$NON-NLS-1$
		}
		catch (ToolException e)
		{
			assertTrue(e.getMessage().contains("java.lang.System")); //$NON-NLS-1$
		}
	}

	@Test
	public void testSimpleVariableDeclaration() throws ToolException
	{
		String code = "int x = 5;\nString s = \"hello\";"; //$NON-NLS-1$
		validator.validate(code);
	}

	@Test
	public void testMethodDefinition() throws ToolException
	{
		String code = "public void test() {\n    int x = 5;\n}"; //$NON-NLS-1$
		validator.validate(code);
	}

	@Test
	public void testLambdaExpression() throws ToolException
	{
        String code = "Runnable r = () -> { String s = \"test\"; };"; //$NON-NLS-1$
		validator.validate(code);
	}

	@Test
	public void testClassDefinition() throws ToolException
	{
		String code = "class MyClass {\n    int field;\n}"; //$NON-NLS-1$
		validator.validate(code);
	}

	@Test
	public void testComments() throws ToolException
	{
		String code = "// This is a comment\n/* Multi-line\ncomment */"; //$NON-NLS-1$
		validator.validate(code);
	}

	@Test
	public void testEmptyRestrictedTypes() throws ToolException
	{
        IRestrictedTypesProvider emptyProvider = new TestRestrictedTypesProvider(new HashSet<>());
        RestrictedTypesValidator emptyValidator = new RestrictedTypesValidator(emptyProvider);

		String code = "import java.io.File;\nFile f = new File(\"test\");"; //$NON-NLS-1$
		emptyValidator.validate(code);
	}

	@Test
	public void testPartialMatchOfRestrictedType()
	{
		String code = "MyFile file = new MyFile();"; //$NON-NLS-1$
		try
		{
			validator.validate(code);
			// Should pass because "MyFile" is not "java.io.File"
		}
		catch (ToolException e)
		{
			fail("ToolException should not be thrown for non-restricted type with similar name"); //$NON-NLS-1$
		}
	}

	@Test
	public void testMultipleRestrictedTypesInCode()
	{
		String code = "import java.io.File;\nimport java.lang.System;\n"; //$NON-NLS-1$
		try
		{
			validator.validate(code);
			fail("ToolException should be thrown for first restricted type"); //$NON-NLS-1$
		}
		catch (ToolException e)
		{
			assertTrue(e.getMessage().contains("java.io.File")); //$NON-NLS-1$
		}
	}

	@Test
	public void testJavaNioFiles()
	{
		String code = "import java.nio.file.Files;"; //$NON-NLS-1$
		try
		{
			validator.validate(code);
			fail("ToolException should be thrown for java.nio.file.Files"); //$NON-NLS-1$
		}
		catch (ToolException e)
		{
			assertTrue(e.getMessage().contains("java.nio.file")); //$NON-NLS-1$
		}
	}

	@Test
	public void testJavaNioPath()
	{
		String code = "java.nio.file.Path p = java.nio.file.Paths.get(\"/tmp\");"; //$NON-NLS-1$
		try
		{
			validator.validate(code);
			fail("ToolException should be thrown for java.nio.file.Path"); //$NON-NLS-1$
		}
		catch (ToolException e)
		{
			assertTrue(e.getMessage().contains("java.nio.file")); //$NON-NLS-1$
		}
	}

    @Test
    public void testRestrictedTypeInArray()
    {
        String code = "File[] files = new File[10];"; //$NON-NLS-1$
        try
        {
            validator.validate(code);
            fail("ToolException should be thrown for File array"); //$NON-NLS-1$
        }
        catch (ToolException e)
        {
            assertTrue(e.getMessage().contains("java.io.File")); //$NON-NLS-1$
        }
    }

    @Test
    public void testRestrictedTypeInGenerics()
    {
        String code = "List<File> fileList;"; //$NON-NLS-1$
        try
        {
            validator.validate(code);
            fail("ToolException should be thrown for File in generics"); //$NON-NLS-1$
        }
        catch (ToolException e)
        {
            assertTrue(e.getMessage().contains("java.io.File")); //$NON-NLS-1$
        }
    }

    @Test
    public void testRestrictedTypeAsMethodParameter()
    {
        String code = "public void process(File file) { }"; //$NON-NLS-1$
        try
        {
            validator.validate(code);
            fail("ToolException should be thrown for File parameter"); //$NON-NLS-1$
        }
        catch (ToolException e)
        {
            assertTrue(e.getMessage().contains("java.io.File")); //$NON-NLS-1$
        }
    }

    @Test
    public void testRestrictedTypeAsReturnType()
    {
        String code = "public File getFile() { return null; }"; //$NON-NLS-1$
        try
        {
            validator.validate(code);
            fail("ToolException should be thrown for File return type"); //$NON-NLS-1$
        }
        catch (ToolException e)
        {
            assertTrue(e.getMessage().contains("java.io.File")); //$NON-NLS-1$
        }
    }

    @Test
    public void testTryWithResourcesRestrictedType()
    {
        String code = "try (FileInputStream fis = new FileInputStream(\"test\")) { }"; //$NON-NLS-1$
        try
        {
            validator.validate(code);
            fail("ToolException should be thrown for FileInputStream in try-with-resources"); //$NON-NLS-1$
        }
        catch (ToolException e)
        {
            assertTrue(e.getMessage().contains("java.io.FileInputStream")); //$NON-NLS-1$
        }
    }

    @Test
    public void testMultipleStaticImports()
    {
        String code = "import static java.lang.System.out;\nimport static java.lang.System.err;"; //$NON-NLS-1$
        try
        {
            validator.validate(code);
            fail("ToolException should be thrown for static imports"); //$NON-NLS-1$
        }
        catch (ToolException e)
        {
            assertTrue(e.getMessage().contains("java.lang.System")); //$NON-NLS-1$
        }
    }

    @Test
    public void testReflectionAccess()
    {
        String code = "java.lang.reflect.Method m = null;"; //$NON-NLS-1$
        try
        {
            validator.validate(code);
            fail("ToolException should be thrown for reflection type"); //$NON-NLS-1$
        }
        catch (ToolException e)
        {
            assertTrue(e.getMessage().contains("java.lang.reflect")); //$NON-NLS-1$
        }
    }

    @Test
    public void testNestedTypeFromRestrictedPackage()
    {
        String code = "import java.nio.file.attribute.FileAttributes;"; //$NON-NLS-1$
        try
        {
            validator.validate(code);
            fail("ToolException should be thrown for nested type from restricted package"); //$NON-NLS-1$
        }
        catch (ToolException e)
        {
            assertTrue(e.getMessage().contains("java.nio.file")); //$NON-NLS-1$
        }
    }

    @Test
    public void testQualifiedTypeNameWithoutImport()
    {
        String code = "void method() {\n    java.io.File f = null;\n}"; //$NON-NLS-1$
        try
        {
            validator.validate(code);
            fail("ToolException should be thrown for qualified type"); //$NON-NLS-1$
        }
        catch (ToolException e)
        {
            assertTrue(e.getMessage().contains("java.io.File")); //$NON-NLS-1$
        }
    }

    @Test
    public void testChainedMethodCallOnRestrictedType()
    {
        String code = "System.exit(0).equals(null);"; //$NON-NLS-1$
        try
        {
            validator.validate(code);
            fail("ToolException should be thrown for chained method call"); //$NON-NLS-1$
        }
        catch (ToolException e)
        {
            assertTrue(e.getMessage().contains("java.lang.System")); //$NON-NLS-1$
        }
    }

    @Test
    public void testSystemGetProperty()
    {
        String code = "String value = System.getProperty(\"key\");"; //$NON-NLS-1$
        try
        {
            validator.validate(code);
            fail("ToolException should be thrown for System.getProperty"); //$NON-NLS-1$
        }
        catch (ToolException e)
        {
            assertTrue(e.getMessage().contains("java.lang.System")); //$NON-NLS-1$
        }
    }

    @Test
    public void testSystemCurrentTimeMillis()
    {
        String code = "long time = System.currentTimeMillis();"; //$NON-NLS-1$
        try
        {
            validator.validate(code);
            fail("ToolException should be thrown for System.currentTimeMillis"); //$NON-NLS-1$
        }
        catch (ToolException e)
        {
            assertTrue(e.getMessage().contains("java.lang.System")); //$NON-NLS-1$
        }
    }

    @Test
    public void testFileOutputStreamCreation()
    {
        String code = "new FileOutputStream(\"output.txt\");"; //$NON-NLS-1$
        try
        {
            validator.validate(code);
            fail("ToolException should be thrown for FileOutputStream"); //$NON-NLS-1$
        }
        catch (ToolException e)
        {
            assertTrue(e.getMessage().contains("java.io.FileOutputStream")); //$NON-NLS-1$
        }
    }

    @Test
    public void testFileReaderCreation()
    {
        String code = "new FileReader(\"input.txt\");"; //$NON-NLS-1$
        try
        {
            validator.validate(code);
            fail("ToolException should be thrown for FileReader"); //$NON-NLS-1$
        }
        catch (ToolException e)
        {
            assertTrue(e.getMessage().contains("java.io.FileReader")); //$NON-NLS-1$
        }
    }

    @Test
    public void testAnonymousClassWithRestrictedType()
    {
        String code = "Runnable r = new Runnable() { public void run() { File f = null; } };"; //$NON-NLS-1$
        try
        {
            validator.validate(code);
            fail("ToolException should be thrown for File in anonymous class"); //$NON-NLS-1$
        }
        catch (ToolException e)
        {
            assertTrue(e.getMessage().contains("java.io.File")); //$NON-NLS-1$
        }
    }

    @Test
    public void testMultipleLinesOfCodeWithMixedTypes()
    {
        String code = "String s = \"test\";\nFile f = new File(\"test\");\nint x = 5;"; //$NON-NLS-1$
        try
        {
            validator.validate(code);
            fail("ToolException should be thrown for File in mixed code"); //$NON-NLS-1$
        }
        catch (ToolException e)
        {
            assertTrue(e.getMessage().contains("java.io.File")); //$NON-NLS-1$
        }
    }

    @Test
    public void testWildcardImportFromRestrictedPackage()
    {
        String code = "import java.nio.file.*;"; //$NON-NLS-1$
        try
        {
            validator.validate(code);
            fail("ToolException should be thrown for wildcard import from restricted package"); //$NON-NLS-1$
        }
        catch (ToolException e)
        {
            assertTrue(e.getMessage().contains("java.nio.file")); //$NON-NLS-1$
        }
    }

    @Test
    public void testComplexCastExpression()
    {
        String code = "(java.io.File)((Object)\"test\");"; //$NON-NLS-1$
        try
        {
            validator.validate(code);
            fail("ToolException should be thrown for complex cast"); //$NON-NLS-1$
        }
        catch (ToolException e)
        {
            assertTrue(e.getMessage().contains("java.io.File")); //$NON-NLS-1$
        }
    }

    @Test
    public void testStaticBlockWithRestrictedType()
    {
        String code = "static { File f = new File(\"test\"); }"; //$NON-NLS-1$
        try
        {
            validator.validate(code);
            fail("ToolException should be thrown for File in static block"); //$NON-NLS-1$
        }
        catch (ToolException e)
        {
            assertTrue(e.getMessage().contains("java.io.File")); //$NON-NLS-1$
        }
    }

    @Test
    public void testInstanceInitializerWithRestrictedType()
    {
        String code = "{ File f = new File(\"test\"); }"; //$NON-NLS-1$
        try
        {
            validator.validate(code);
            fail("ToolException should be thrown for File in instance initializer"); //$NON-NLS-1$
        }
        catch (ToolException e)
        {
            assertTrue(e.getMessage().contains("java.io.File")); //$NON-NLS-1$
        }
    }

    @Test
    public void testInterfaceMethodWithRestrictedType()
    {
        String code = "interface MyInterface { File getFile(); }"; //$NON-NLS-1$
        try
        {
            validator.validate(code);
            fail("ToolException should be thrown for File in interface"); //$NON-NLS-1$
        }
        catch (ToolException e)
        {
            assertTrue(e.getMessage().contains("java.io.File")); //$NON-NLS-1$
        }
    }

    @Test
    public void testNestedClassDeclarationWithRestrictedType()
    {
        String code = "class Outer { class Inner { File f; } }"; //$NON-NLS-1$
        try
        {
            validator.validate(code);
            fail("ToolException should be thrown for File in nested class"); //$NON-NLS-1$
        }
        catch (ToolException e)
        {
            assertTrue(e.getMessage().contains("java.io.File")); //$NON-NLS-1$
        }
    }

    @Test
    public void testGenericTypeVariableWithRestrictedType()
    {
        String code = "<T extends File> void genericMethod() { }"; //$NON-NLS-1$
        try
        {
            validator.validate(code);
            fail("ToolException should be thrown for File in generic type variable"); //$NON-NLS-1$
        }
        catch (ToolException e)
        {
            assertTrue(e.getMessage().contains("java.io.File")); //$NON-NLS-1$
        }
    }

    @Test
    public void testMultipleRestrictedTypesInLine()
    {
        String code = "File f = new File(\"test\"); System.exit(0);"; //$NON-NLS-1$
        try
        {
            validator.validate(code);
            fail("ToolException should be thrown for first restricted type"); //$NON-NLS-1$
        }
        catch (ToolException e)
        {
            // Should catch first one
            assertTrue(e.getMessage().contains("File") || e.getMessage().contains("System")); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    @Test
    public void testRestrictedTypeInFieldDeclaration()
    {
        String code = "class MyClass { File file; }"; //$NON-NLS-1$
        try
        {
            validator.validate(code);
            fail("ToolException should be thrown for File field"); //$NON-NLS-1$
        }
        catch (ToolException e)
        {
            assertTrue(e.getMessage().contains("java.io.File")); //$NON-NLS-1$
        }
    }

    @Test
    public void testRestrictedTypeInStaticFieldDeclaration()
    {
        String code = "class MyClass { static File staticFile; }"; //$NON-NLS-1$
        try
        {
            validator.validate(code);
            fail("ToolException should be thrown for File static field"); //$NON-NLS-1$
        }
        catch (ToolException e)
        {
            assertTrue(e.getMessage().contains("java.io.File")); //$NON-NLS-1$
        }
    }

    @Test
    public void testRestrictedTypeInLocalVariableInsideBlock()
    {
        String code = "{ File f = null; }"; //$NON-NLS-1$
        try
        {
            validator.validate(code);
            fail("ToolException should be thrown for File in block"); //$NON-NLS-1$
        }
        catch (ToolException e)
        {
            assertTrue(e.getMessage().contains("java.io.File")); //$NON-NLS-1$
        }
    }
}
