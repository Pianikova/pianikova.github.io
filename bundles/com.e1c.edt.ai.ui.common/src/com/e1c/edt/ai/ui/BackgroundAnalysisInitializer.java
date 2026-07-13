/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.ui;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Инициализатор фонового анализа изменений кода.
 * Регистрирует триггер анализа при запуске плагина.
 *
 * @author Bogdan Sushkov
 */
@Singleton
public class BackgroundAnalysisInitializer
    implements IInitializable
{
    private final BackgroundAnalysisRegistrar registrar;

    @Inject
    public BackgroundAnalysisInitializer(BackgroundAnalysisRegistrar registrar)
    {
        this.registrar = registrar;
    }

    @Override
    public void initialize()
    {
        registrar.register();
    }
}
