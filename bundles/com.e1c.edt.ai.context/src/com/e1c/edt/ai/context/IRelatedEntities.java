/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context;

import java.util.Optional;

import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.context.DTO.RelatedEntitiesRequest;
import com.e1c.edt.ai.context.DTO.RelatedEntitiesResponse;

public interface IRelatedEntities
{
    Optional<RelatedEntitiesResponse> getRelatedEntities(RelatedEntitiesRequest request,
        ICancellationToken cancellationToken);
}
