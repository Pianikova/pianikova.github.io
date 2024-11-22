/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

public class AssistentModule
    extends AbstractModule
{
    @Override
    protected void configure()
    {
        // @formatter:off
        bind(IEnvironment.class).to(Environment.class).in(Singleton.class);
        // @formatter:on
    }
}
