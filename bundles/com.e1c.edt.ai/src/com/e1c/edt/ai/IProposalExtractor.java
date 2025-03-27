/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import java.util.Optional;

public interface IProposalExtractor
{
    Optional<String> extract(String prefix, String proposal);
}
