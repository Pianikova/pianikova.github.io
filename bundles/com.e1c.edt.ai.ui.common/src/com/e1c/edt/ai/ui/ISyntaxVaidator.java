/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.ICancellationToken;

interface ISyntaxVaidator
{
    int getValidHintSize(String filePath, String code, String hint, int offset, ICancellationToken cancellationToken);
}
