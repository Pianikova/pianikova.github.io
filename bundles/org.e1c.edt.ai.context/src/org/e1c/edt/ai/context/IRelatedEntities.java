/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.context;

import java.util.Optional;

public interface IRelatedEntities
{
    Optional<RelatedEntitiesResponse> getRelatedEntities(RelatedEntitiesRequest request);
}
