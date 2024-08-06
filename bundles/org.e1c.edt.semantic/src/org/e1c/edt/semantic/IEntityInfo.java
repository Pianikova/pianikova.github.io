/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.semantic;

import java.util.Optional;

public interface IEntityInfo
{
    Optional<EntityInfoResponse> geInfo(EntityInfoRequest request);
}
