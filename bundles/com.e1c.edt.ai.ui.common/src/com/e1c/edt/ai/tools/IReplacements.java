package com.e1c.edt.ai.tools;

public interface IReplacements
{

	String[] splitLines(String text);

	String[] removeTrailingEmptyLine(String[] lines);

	String blockByLineRange(String content, String[] lines, int startLine, int endLine);
}
