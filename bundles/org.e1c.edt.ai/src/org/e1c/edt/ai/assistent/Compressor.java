/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.zip.GZIPOutputStream;

import org.e1c.edt.ai.ILog;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

class Compressor
    implements ICompressor
{
    private final ILog log;

    @Inject
    public Compressor(ILog log)
    {
        Preconditions.checkNotNull(log);
        this.log = log;
    }

    @Override
    public Optional<ByteArrayOutputStream> compress(String str)
    {
        ByteArrayOutputStream obj = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(obj))
        {
            gzip.write(str.getBytes("UTF-8")); //$NON-NLS-1$
            gzip.close();
        }
        catch (IOException error)
        {
            log.logError(error);
            return Optional.empty();
        }

        return Optional.of(obj);
    }
}
