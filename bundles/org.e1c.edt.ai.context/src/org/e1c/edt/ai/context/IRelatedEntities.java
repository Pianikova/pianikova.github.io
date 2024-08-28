/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.context;

import java.util.Optional;

import org.e1c.edt.ai.ICancellationToken;

public interface IRelatedEntities
{
    Optional<RelatedEntitiesResponse> getRelatedEntities(RelatedEntitiesRequest request,
        ICancellationToken cancellationToken);
}
