/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai.context;

import java.util.Optional;

import org.e1c.edt.ai.ICancellationToken;
import org.e1c.edt.ai.context.DTO.EntityInfoRequest;
import org.e1c.edt.ai.context.DTO.EntityInfoResponse;

public interface IEntityInfo
{
    Optional<EntityInfoResponse> getInfo(EntityInfoRequest request, ICancellationToken cancellationToken);
}
