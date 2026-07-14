/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.ui.preferences;

import java.util.Arrays;

import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;

import com.e1c.edt.ai.ISettingsStore;
import com.e1c.edt.ai.assistent.model.CodeCompletionPolicy;

/**
 * Страница настроек завершения кода: политика завершения и количество строк подсказки.
 *
 * @author Nikolay Pianikov
 */
public class CodeCompletionPreferencePage
    extends BaseAIPreferencePage
{
    @Override
    public void createFieldEditors()
    {
        var parent = getFieldEditorParent();

        var policyCombo = new PolicyComboFieldEditor(parent);
        setLabelTooltip(policyCombo, parent, Messages.ClientAIPreferencePage_CodeCompletionPolicy_Tooltip);
        addField(policyCombo);

        var codeCompletionLinesCount = new IntegerFieldEditor(ISettingsStore.CODE_COMPLETION_LINES_COUNT,
            Messages.ClientAIPreferencePage_CodeCompletionLinesCount, parent);
        codeCompletionLinesCount.setValidRange(1, ISettingsStore.MAX_CODE_COMPLETION_LINES_COUNT);
        setLabelTooltip(codeCompletionLinesCount, parent,
            Messages.ClientAIPreferencePage_CodeCompletionLinesCount_Tooltip);
        addField(codeCompletionLinesCount);
    }

    static class PolicyComboFieldEditor
        extends ComboFieldEditor
    {
        private static final String[][] CODE_COMPLETION_POLICIES = Arrays.stream(CodeCompletionPolicy.values())
            .map(policy -> new String[] { policy.getLongName(), policy.getId() })
            .toArray(String[][]::new);
        private Combo combo;

        PolicyComboFieldEditor(Composite parent)
        {
            super(ISettingsStore.CODE_COMPLETION_POLICY, Messages.ClientAIPreferencePage_CodeCompletionPolicy,
                CODE_COMPLETION_POLICIES, parent);
        }

        @Override
        protected void doFillIntoGrid(Composite parent, int numColumns)
        {
            super.doFillIntoGrid(parent, numColumns);
            var childernAfter = parent.getChildren();
            if (childernAfter.length > 0)
            {
                var control = childernAfter[childernAfter.length - 1];
                if (control instanceof Combo)
                {
                    combo = (Combo)control;
                }
            }
        }

        @Override
        protected void doLoad()
        {
            super.doLoad();
            updateToolTipText();
        }

        @Override
        protected void doLoadDefault()
        {
            super.doLoadDefault();
            updateToolTipText();
        }

        @Override
        protected void valueChanged(String oldValue, String newValue)
        {
            super.valueChanged(oldValue, newValue);
            updateToolTipText();
        }

        private void updateToolTipText()
        {
            if (combo == null)
            {
                return;
            }

            var index = combo.getSelectionIndex();
            var policies = CodeCompletionPolicy.values();
            if (policies != null && policies.length > 0 && index >= 0 && index < policies.length)
            {
                combo.setToolTipText(policies[index].getDescription());
            }
            else
            {
                combo.setToolTipText(""); //$NON-NLS-1$
            }
        }
    }
}
