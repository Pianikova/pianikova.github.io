/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui.preferences;

import java.util.concurrent.CompletableFuture;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.IStateService;
import com.e1c.edt.ai.IValidator;
import com.e1c.edt.ai.ServiceState;
import com.e1c.edt.ai.assistent.ITokenCheck;
import com.e1c.edt.ai.ui.IDispatcher;
import com.google.inject.Inject;

/**
 * Custom field editor for token with validation button
 * @author Bogdan Sushkov
 *
 */
class TokenFieldEditor
    extends ValidatingStringFieldEditor
{
    @Inject
    private ILog log;

    @Inject
    private ITokenCheck tokenCheck;

    @Inject
    private IStateService stateService;

    @Inject
    private IDispatcher dispatcher;

    private Button validateButton;

    public TokenFieldEditor(String name, String labelText, Composite parent, IValidator<String> validator)
    {
        super(name, labelText, parent, validator);
        // Allow empty strings to prevent blocking page opening
        setEmptyStringAllowed(true);
    }

    @Override
    protected void doFillIntoGrid(Composite parent, int numColumns)
    {
        // Label
        getLabelControl(parent);

        // Text control
        var textControl = getTextControl(parent);
        var gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gridData.horizontalSpan = numColumns - 2;
        textControl.setLayoutData(gridData);
        textControl.setFont(parent.getFont());

        // Validate button
        validateButton = new Button(parent, SWT.PUSH);
        validateButton.setText(Messages.TokenFieldEditor_Validate);
        validateButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
        validateButton.setEnabled(true);

        validateButton.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                validateToken();
            }
        });
    }

    @Override
    public int getNumberOfControls()
    {
        return 3; // label + text control + button
    }

    @Override
    protected void adjustForNumColumns(int numColumns)
    {
        var labelControl = getLabelControl();
        var gridData = (GridData)labelControl.getLayoutData();
        if (gridData == null)
        {
            gridData = new GridData();
            labelControl.setLayoutData(gridData);
        }
        gridData.horizontalSpan = 1;

        var textControl = getTextControl();
        gridData = (GridData)textControl.getLayoutData();
        if (gridData == null)
        {
            gridData = new GridData();
            textControl.setLayoutData(gridData);
        }
        gridData.horizontalSpan = numColumns - 2;
    }

    @Override
    public boolean doCheckState()
    {
        return super.doCheckState();
    }

    private void validateToken()
    {
        // Validate token format first
        if (!doCheckState())
        {
            MessageDialog.openError(validateButton.getShell(), Messages.TokenFieldEditor_ValidationError,
                getErrorMessage());
            return;
        }

        // Set state before validation
        if (stateService != null)
        {
            stateService.setState(ServiceState.SETTINGS_CHANGED);
        }

        // Disable button during validation
        validateButton.setEnabled(false);

        // Perform async validation with token check service
        if (tokenCheck != null)
        {
            var token = getStringValue();
            CompletableFuture.runAsync(() -> {
                try
                {
                    var isValid = tokenCheck.checkTokenAsync(token).get();

                    dispatcher.dispatchAsync(() -> {
                        if (isValid)
                        {
                            MessageDialog.openInformation(validateButton.getShell(), Messages.TokenFieldEditor_ValidationSuccess,
                                Messages.TokenFieldEditor_TokenValid);
                        }
                        else
                        {
                            MessageDialog.openError(validateButton.getShell(), Messages.TokenFieldEditor_ValidationError,
                                Messages.TokenFieldEditor_TokenInvalid);
                        }
                    });
                }
                catch (Exception ex)
                {
                    if (log != null)
                    {
                        log.logError("Token validation failed: " + ex.getMessage()); //$NON-NLS-1$
                    }
                    dispatcher.dispatchAsync(() -> {
                        MessageDialog.openError(validateButton.getShell(), Messages.TokenFieldEditor_ValidationError,
                            Messages.TokenFieldEditor_TokenInvalid);
                    });
                }
                finally
                {
                    dispatcher.dispatchAsync(() -> {
                        validateButton.setEnabled(true);
                    });
                }
            });
        }
        else
        {
            MessageDialog.openError(validateButton.getShell(), Messages.TokenFieldEditor_ValidationError,
                Messages.TokenFieldEditor_TokenInvalid);
            validateButton.setEnabled(true);
        }
    }
}
