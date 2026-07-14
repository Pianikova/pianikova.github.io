/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.ui.preferences;

import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.e1c.edt.ai.IStateService;
import com.e1c.edt.ai.ServiceState;
import com.e1c.edt.ai.ui.BaseActivator;
import com.google.inject.Inject;

/**
 * Базовый класс дочерних страниц настроек «1С:Напарник». Подключает общий preference store,
 * отслеживает изменения и по OK уведомляет сервис состояния, чтобы новые значения вступили в силу.
 *
 * @author Nikolay Pianikov
 */
public abstract class BaseAIPreferencePage
    extends FieldEditorPreferencePage
    implements IWorkbenchPreferencePage
{
    @Inject
    IPreferenceStore preferenceStore;
    @Inject
    IStateService stateService;

    private boolean settingsChanged;

    protected BaseAIPreferencePage()
    {
        super(GRID);
        BaseActivator.injectMembers(this);
        setPreferenceStore(preferenceStore);
    }

    @Override
    public void init(IWorkbench workbench)
    {
        // nothing to initialize
    }

    @Override
    public void propertyChange(PropertyChangeEvent event)
    {
        super.propertyChange(event);
        settingsChanged = true;
    }

    @Override
    public boolean performOk()
    {
        var result = super.performOk();
        if (settingsChanged)
        {
            stateService.setState(ServiceState.SETTINGS_CHANGED);
        }
        return result;
    }

    protected void setLabelTooltip(FieldEditor editor, Composite parent, String tooltip)
    {
        editor.getLabelControl(parent).setToolTipText(tooltip);
    }

    /**
     * Sets a tooltip on the checkbox of a {@code BooleanFieldEditor}. The editor keeps its label as
     * the checkbox text (no separate label control), so the tooltip is applied to the {@link Button}
     * directly instead of via {@code getLabelControl}, which would create a duplicate label.
     */
    protected void setCheckboxTooltip(Composite parent, String tooltip)
    {
        for (var child : parent.getChildren())
        {
            if (child instanceof Button)
            {
                child.setToolTipText(tooltip);
            }
        }
    }
}
