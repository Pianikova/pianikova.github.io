/*
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent;

import org.e1c.edt.ai.assistent.model.AITextResponse;

/**
 * @author Bogdan Sushkov
 *
 */
public interface IAICodeAssistant
{

    /**
     *
     * TODO JavaDoc
     *
     * @param text
     * @return
     */
    public AITextResponse generateText(String text, CancellationToken cancellationToken);
}
