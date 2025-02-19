/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui.preferences;

import java.util.TreeMap;

import org.e1c.edt.ai.IValidator;
import org.e1c.edt.ai.WellknownError;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;

class ValidatingStringFieldEditor
    extends StringFieldEditor
{
    private static final TreeMap<WellknownError, String> Errors = new TreeMap<>();
    private IValidator<String> validator;

    static
    {
        Errors.put(WellknownError.UnableToParse, Messages.Error_UnableToParse);
        Errors.put(WellknownError.OutOfRange, Messages.Error_OutOfRange);
        Errors.put(WellknownError.Unknown, Messages.Error_Unknown);
    }

    public ValidatingStringFieldEditor(String name, String labelText, Composite parent, IValidator<String> validator)
    {
        super(name, labelText, parent);
        this.validator = validator;
    }

    @Override
    public boolean doCheckState()
    {
        if (!super.doCheckState())
        {
            return false;
        }

        var validationResult = validator.validate(getStringValue());
        if (validationResult.getErrors().isEmpty())
        {
            return true;
        }

        var stringBuilders = new TreeMap<WellknownError, StringBuilder>();
        for (var validationError : validationResult.getErrors())
        {
            var error = validationError.getError();
            var message = Errors.get(error);
            if (message == null)
            {
                continue;
            }

            StringBuilder stringBuilder = stringBuilders.computeIfAbsent(error, k -> new StringBuilder());
            if (stringBuilder.length() == 0)
            {
                stringBuilder.append(message);
                stringBuilder.append(": "); //$NON-NLS-1$
            }
            else
            {
                stringBuilder.append(", "); //$NON-NLS-1$
            }

            stringBuilder.append(validationError.getTarget());
        }

        StringBuilder errors = new StringBuilder();
        for (var stringBuilder : stringBuilders.values())
        {
            if (errors.length() > 0)
            {
                errors.append(". "); //$NON-NLS-1$
            }

            errors.append(stringBuilder);
        }


        setErrorMessage(errors.toString());
        return false;
    }

}
