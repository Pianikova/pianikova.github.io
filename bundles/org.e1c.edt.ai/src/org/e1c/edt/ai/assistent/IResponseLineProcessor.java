/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent;

import org.e1c.edt.ai.IObserver;

public interface IResponseLineProcessor
{
    boolean process(IObserver<String> observer, String line);
}
