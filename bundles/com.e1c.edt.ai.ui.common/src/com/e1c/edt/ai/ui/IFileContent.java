/**
 *
 */
package com.e1c.edt.ai.ui;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Optional;

import com.e1c.edt.ai.assistent.model.ProjectId;

public interface IFileContent
{
    ProjectId getProjectId();

    Charset getCharset();

    Optional<InputStream> getInputStream();

    Optional<OutputStream> getOutputStream();
}
