/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent;

import java.io.ByteArrayOutputStream;
import java.util.Optional;

public interface ICompressor
{
    Optional<ByteArrayOutputStream> compress(String str);
}
