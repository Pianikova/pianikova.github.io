/**
 *
 */
package com.e1c.edt.ai.ui;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Optional;

import com.e1c.edt.ai.assistent.model.ProjectId;
import com.google.common.base.Preconditions;

public class PathContent
    implements IFileContent
{
    private final Path path;

    public PathContent(Path path)
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
    public Charset getCharset()
    {
        return Charset.defaultCharset();
    }

    @Override
    public Optional<InputStream> getInputStream()
    {
        try
        {
            return Optional.ofNullable(new FileInputStream(path.toString()));
        }
        catch (FileNotFoundException e)
        {
            return Optional.empty();
        }
    }

    @Override
    public String toString()
    {
        return path.toString();
    }
}
