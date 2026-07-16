/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.ui.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;

import com.e1c.edt.ai.ISettingsStore;
import com.e1c.edt.ai.assistent.model.ProblemLevel;

/**
 * Страница настроек фонового анализа кода: включение и минимальный уровень проблем.
 *
 * @author Nikolay Pianikov
 */
public class BackgroundAnalysisPreferencePage
    extends BaseAIPreferencePage
{
    // Order matters: entries go from least to most severe threshold. Value = ProblemLevel id.
    private static final String[][] PROBLEM_LEVELS = {
        { Messages.ClientAIPreferencePage_ProblemLevel_Information, ProblemLevel.INFORMATION_ID },
        { Messages.ClientAIPreferencePage_ProblemLevel_Warnings, ProblemLevel.WARNING_ID },
        { Messages.ClientAIPreferencePage_ProblemLevel_Errors, ProblemLevel.ERROR_ID } };

    @Override
    public void createFieldEditors()
    {
        var parent = getFieldEditorParent();

        var backgroundAnalysisField = new BooleanFieldEditor(ISettingsStore.BACKGROUND_ANALYSIS,
            Messages.ClientAIPreferencePage_BackgroundAnalysis, parent);
        addField(backgroundAnalysisField);
        setCheckboxTooltip(parent, Messages.ClientAIPreferencePage_BackgroundAnalysis_Tooltip);

        var problemLevelField = new ComboFieldEditor(ISettingsStore.BACKGROUND_ANALYSIS_PROBLEM_LEVEL,
            Messages.ClientAIPreferencePage_ProblemLevel, PROBLEM_LEVELS, parent);
        setLabelTooltip(problemLevelField, parent, Messages.ClientAIPreferencePage_ProblemLevel_Tooltip);
        addField(problemLevelField);
    }
}
