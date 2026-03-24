/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.assistent;

import java.util.List;


/**
 * @author Bogdan Sushkov
 *
 */
public interface IDiagnosticsFactory
{

    /**
     * Создает и возвращает список диагностических тестов для проверки состояния системы.
     *
     * @return список диагностических тестов типа {@link IDiagnosticTest}.
     */
    List<IDiagnosticTest> createDiagnostics();

}
