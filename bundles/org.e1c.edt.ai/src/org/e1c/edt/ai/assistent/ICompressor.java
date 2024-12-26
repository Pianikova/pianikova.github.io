/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent;

import java.io.ByteArrayOutputStream;
import java.util.Optional;

public interface ICompressor
{
    Optional<ByteArrayOutputStream> compress(String str);
}
