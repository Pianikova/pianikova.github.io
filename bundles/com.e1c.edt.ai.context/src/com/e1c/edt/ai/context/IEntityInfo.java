/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context;

import java.util.Optional;

import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.context.DTO.EntityInfoRequest;
import com.e1c.edt.ai.context.DTO.EntityInfoResponse;

public interface IEntityInfo
{
    Optional<EntityInfoResponse> getInfo(EntityInfoRequest request, ICancellationToken cancellationToken);
}
