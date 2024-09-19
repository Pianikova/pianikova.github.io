/*
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent;

import org.e1c.edt.ai.ServerAccessType;

/**
 * @author Bogdan Sushkov
 *
 */
public interface IServerAccessListener
{
    void onServerAccessChange(ServerAccessType currentStatus);
}
