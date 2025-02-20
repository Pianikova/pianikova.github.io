/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai.ui.handlers;

import java.util.Optional;

public interface ICodeTools
{
    Optional<TargetMethod> getTargetMethod();

    void selectMethodComment(TargetMethod commentingMethod);
}
