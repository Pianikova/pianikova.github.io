/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai.assistent;

import org.e1c.edt.ai.IObserver;
import org.e1c.edt.ai.assistent.model.Completion;

public interface IResponseLineProcessor
{
    boolean process(IObserver<Completion> observer, String line);
}
