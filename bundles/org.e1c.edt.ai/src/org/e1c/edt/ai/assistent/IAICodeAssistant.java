/*
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent;

import java.util.Optional;

import org.e1c.edt.ai.assistent.model.AITextResponse;

/**
 * @author Bogdan Sushkov
 *
 */
public interface IAICodeAssistant
{
    public Optional<AITextResponse> generateText(String text, CancellationToken cancellationToken);
}
