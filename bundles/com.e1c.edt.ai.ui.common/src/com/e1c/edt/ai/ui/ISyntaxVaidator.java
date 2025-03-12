/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.AIContext;
import com.e1c.edt.ai.CodeMethod;
import com.e1c.edt.ai.ICancellationToken;

interface ISyntaxVaidator
{
    String getValidHint(CodeMethod method, AIContext aiContext, String hintText,
        ICancellationToken cancellationToken);
}
