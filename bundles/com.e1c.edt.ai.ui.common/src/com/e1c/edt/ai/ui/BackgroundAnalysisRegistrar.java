/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.ui;

import org.eclipse.core.resources.ResourcesPlugin;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Регистратор триггера фонового анализа изменений.
 * При создании подписывается на события изменения файлов,
 * при удалении - отписывается.
 *
 * @author Bogdan Sushkov
 */
@Singleton
public class BackgroundAnalysisRegistrar
{
    private final BackgroundAnalysisTrigger trigger;
    private boolean registered;

    @Inject
    public BackgroundAnalysisRegistrar(BackgroundAnalysisTrigger analysisTrigger)
    {
        this.trigger = analysisTrigger;
        this.registered = false;
    }

    /**
     * Регистрирует триггер для отслеживания изменений файлов.
     */
    public void register()
    {
        if (!registered)
        {
            ResourcesPlugin.getWorkspace().addResourceChangeListener(trigger);
            registered = true;
        }
    }

    /**
     * Отменяет регистрацию триггера.
     */
    public void unregister()
    {
        if (registered)
        {
            ResourcesPlugin.getWorkspace().removeResourceChangeListener(trigger);
            registered = false;
        }
    }

    /**
     * Проверяет, зарегистрирован ли триггер.
     *
     * @return true, если триггер зарегистрирован
     */
    public boolean isRegistered()
    {
        return registered;
    }
}
