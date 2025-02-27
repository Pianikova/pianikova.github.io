/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai.context;

import java.util.Optional;

import org.e1c.edt.ai.ICancellationToken;
import org.e1c.edt.ai.context.DTO.RelatedEntitiesRequest;
import org.e1c.edt.ai.context.DTO.RelatedEntitiesResponse;

public interface IRelatedEntities
{
    Optional<RelatedEntitiesResponse> getRelatedEntities(RelatedEntitiesRequest request,
        ICancellationToken cancellationToken);
}
