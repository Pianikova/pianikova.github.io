/*
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui.preferences;

import java.util.Arrays;
import java.util.Objects;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.e1c.edt.ai.IClientTokenValidator;
import com.e1c.edt.ai.IDefaultSettings;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.ISettingsSetter;
import com.e1c.edt.ai.ISettingsStore;
import com.e1c.edt.ai.IStateService;
import com.e1c.edt.ai.IValidator;
import com.e1c.edt.ai.ServiceState;
import com.e1c.edt.ai.ValidationError;
import com.e1c.edt.ai.ValidationResult;
import com.e1c.edt.ai.WellknownError;
import com.e1c.edt.ai.assistent.model.CodeCompletionPolicy;
import com.e1c.edt.ai.ui.AIUICommonModule;
import com.e1c.edt.ai.ui.BaseActivator;
import com.e1c.edt.ai.ui.IWeb;
import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * This class contains fields on the preferences page with AI chat settings.
 * Parameters set on this page could be used when creating a new chat.
 * @see ClientAI
 * @author Bogdan Sushkov
 *
 */
public class ClientAIPreferencePage
    extends FieldEditorPreferencePage
    implements IWorkbenchPreferencePage
{
    private final Image SPLASH = createImage("icons/obj16/splash.png"); //$NON-NLS-1$

    @SuppressWarnings("nls")
    private static final String[][] LANGUAGES = {
        { Messages.ClientAIPreferencePage_Language_Default, "" },
        { Messages.ClientAIPreferencePage_Language_English, "english" },
        { Messages.ClientAIPreferencePage_Language_Russian, "russian" } };

    @Inject
    ILog log;
    @Inject
    @Named(AIUICommonModule.URL)
    IValidator<String> urlValidator;
    @Inject
    @Named(AIUICommonModule.PARAMETERS)
    IValidator<String> parametersValidator;
    @Inject
    IPreferenceStore preferenceStore;
    @Inject
    IDefaultSettings defaultSettings;
    @Inject
    IStateService stateService;
    @Inject
    ISettings settings;
    @Inject
    ISettingsSetter settingsSetter;
    @Inject
    IClientTokenValidator clientTokenValidator;
    @Inject
    IWeb web;

    private String prevToken;
    private boolean settingsChanged = false;

    public ClientAIPreferencePage()
    {
        super(GRID);
        BaseActivator.injectMembers(this);
        setPreferenceStore(preferenceStore);
    }

    /**
     * Creates the field editors. Field editors are abstractions of
     * the common GUI blocks needed to manipulate various types
     * of preferences. Each field editor knows how to save and
     * restore itself.
     */
    @Override
    public void createFieldEditors()
    {
        var parent = getFieldEditorParent();

        var tokenField = new ValidatingStringFieldEditor(ISettingsStore.CLIENT_TOKEN,
            Messages.ClientAIPreferencePage_Client_Token, parent, new IValidator<String>()
            {
                @Override
                public ValidationResult validate(String token)
                {
                    // Empty or whitespace-only tokens are considered valid
                    if (token == null || token.isBlank())
                    {
                        return ValidationResult.SUCCESS;
                    }

                    // Validate non-empty tokens using the client token validator
                    if (!clientTokenValidator.isValid(token))
                    {
                        return new ValidationResult(new ValidationError(WellknownError.InvalidToken, token));
                    }

                    return ValidationResult.SUCCESS;
                }
            });
        setLabelTooltip(tokenField, Messages.ClientAIPreferencePage_Client_Token_Tooltip);
        var tokenText = tokenField.getTextControl(getFieldEditorParent());
        tokenText.setEchoChar('*');
        addField(tokenField);

        var policyCombo = new PolicyComboFieldEditor(parent);
        setLabelTooltip(policyCombo, Messages.ClientAIPreferencePage_CodeCompletionPolicy_Tooltip);
        addField(policyCombo);

        var codeCompletionLinesCount = new IntegerFieldEditor(ISettingsStore.CODE_COMPLETION_LINES_COUNT,
            Messages.ClientAIPreferencePage_CodeCompletionLinesCount, parent);
        codeCompletionLinesCount.setValidRange(1, ISettingsStore.MAX_CODE_COMPLETION_LINES_COUNT);
        setLabelTooltip(codeCompletionLinesCount, Messages.ClientAIPreferencePage_CodeCompletionLinesCount_Tooltip);
        addField(codeCompletionLinesCount);

        var comboField =
            new ComboFieldEditor(ISettingsStore.LANGUAGE, Messages.ClientAIPreferencePage_Language, LANGUAGES, parent);
        setLabelTooltip(comboField, Messages.ClientAIPreferencePage_Language_Tooltip);
        addField(comboField);

        var validatorField = new ValidatingStringFieldEditor(ISettingsStore.PARAMETERS,
            Messages.ClientAIPreferencePage_Parameters, parent, parametersValidator);
        setLabelTooltip(validatorField, Messages.ClientAIPreferencePage_Parameters_Tooltip);
        addField(validatorField);
    }

    private void setLabelTooltip(org.eclipse.jface.preference.FieldEditor editor, String tooltip)
    {
        var label = editor.getLabelControl(getFieldEditorParent());
        label.setToolTipText(tooltip);
    }

    @Override
    public void init(IWorkbench workbench)
    {
        this.prevToken = settings.getClientToken();
    }

    @Override
    public void propertyChange(PropertyChangeEvent event)
    {
        super.propertyChange(event);
        settingsChanged = true;
    }

    @SuppressWarnings("nls")
    @Override
    protected Control createContents(Composite parent)
    {
        var control = super.createContents(parent);

        var pluginLink = new Link(parent, SWT.NONE);
        pluginLink
            .setText("<a href=\"" + defaultSettings.getHomePage() + "\">" + defaultSettings.getHomePage() + "</a>");
        pluginLink.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                try
                {
                    web.browse(defaultSettings.getHomePage());
                }
                catch (Exception ex)
                {
                    log.logError("Failed to open URL: " + defaultSettings.getHomePage());
                    log.logError(ex);
                }
            }
        });

        var iconLabel = new Label(parent, SWT.NONE);
        iconLabel.setImage(SPLASH);

        return control;
    }

    @Override
    public void dispose()
    {
        SPLASH.dispose();
        super.dispose();
    }

    @Override
    public boolean performOk()
    {
        var result = super.performOk();

        if (settingsChanged)
        {
            var token = settings.getClientToken();
            if (clientTokenValidator.isValid(token) && settings.getCodeCompletionPolicy() == CodeCompletionPolicy.OFF
                && !Objects.equals(token, this.prevToken))
            {
                settingsSetter.setCodeCompletionPolicy(CodeCompletionPolicy.MODERATE);
            }

            stateService.setState(ServiceState.SETTINGS_CHANGED);
        }

        this.prevToken = settings.getClientToken();
        settingsChanged = false;
        return result;
    }

    private static Image createImage(String path)
    {
        var descriptor = ImageDescriptor
            .createFromURL(FileLocator.find(BaseActivator.getDefault().getBundle(), new Path(path), null));
        return descriptor.createImage();
    }

    private static class PolicyComboFieldEditor
        extends ComboFieldEditor
    {
        private static final String[][] CODE_COMPLETION_POLICIES = Arrays.stream(CodeCompletionPolicy.values())
            .map(policy -> new String[] { policy.getLongName(), policy.getId() })
            .toArray(String[][]::new);
        private Combo combo;

        public PolicyComboFieldEditor(Composite parent)
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
