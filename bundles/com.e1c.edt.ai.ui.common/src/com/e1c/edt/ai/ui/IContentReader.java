/**
 *
 */
package com.e1c.edt.ai.ui;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.eclipse.core.runtime.CoreException;

import com.e1c.edt.ai.assistent.model.ProjectId;

public interface IContentReader
{
    ProjectId getProjectId();

    String getName();

    Charset getCharset();

    InputStream getInputStream() throws IOException, CoreException;
}
