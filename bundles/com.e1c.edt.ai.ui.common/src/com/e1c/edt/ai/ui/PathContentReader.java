/**
 *
 */
package com.e1c.edt.ai.ui;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;

import com.e1c.edt.ai.assistent.model.ProjectId;
import com.google.common.base.Preconditions;

public class PathContentReader
    implements IContentReader
{
    private final Path path;

    public PathContentReader(Path path)
    {
        Preconditions.checkNotNull(path);
        this.path = path;
    }

    @Override
    public ProjectId getProjectId()
    {
        return ProjectId.Default;
    }

    @Override
    public String getName()
    {
        return path.toString();
    }

    @Override
    public Charset getCharset()
    {
        return Charset.defaultCharset();
    }

    @Override
    public InputStream getInputStream() throws FileNotFoundException
    {
        var inputStream = new FileInputStream(path.toString());
        return inputStream;
    }
}
