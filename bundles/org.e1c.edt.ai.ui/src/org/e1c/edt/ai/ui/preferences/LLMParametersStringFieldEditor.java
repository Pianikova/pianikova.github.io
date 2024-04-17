/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui.preferences;

import org.e1c.edt.ai.IValidator;
import org.e1c.edt.ai.ParametersParser;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;

public class LLMParametersStringFieldEditor
    extends StringFieldEditor
{
    private IValidator<String> validator;

    public LLMParametersStringFieldEditor(String name, String labelText, Composite parent, IValidator<String> validator)
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

        StringBuilder unableToParseErrors = new StringBuilder();
        StringBuilder unknownParameterErrors = new StringBuilder();
        for (var error : validationResult.getErrors())
        {
            switch (error.getId())
            {
            case ParametersParser.UnableToParseErrorId:
                if (unableToParseErrors.length() == 0)
                {
                    unableToParseErrors.append("Unable to parse: "); //$NON-NLS-1$
                }
                else
                {
                    unableToParseErrors.append(", "); //$NON-NLS-1$
                }

                unableToParseErrors.append(error.getTarget());
                break;

            case ParametersParser.UnknownParameterErrorId:
                if (unknownParameterErrors.length() == 0)
                {
                    unknownParameterErrors.append("Unknown: "); //$NON-NLS-1$
                }
                else
                {
                    unknownParameterErrors.append(", "); //$NON-NLS-1$
                }

                unknownParameterErrors.append(error.getTarget());
                break;
            }
        }

        StringBuilder errors = new StringBuilder();
        errors.append(unableToParseErrors);
        if (errors.length() > 0)
        {
            errors.append(" "); //$NON-NLS-1$
        }

        errors.append(unknownParameterErrors);

        setErrorMessage(errors.toString());
        return false;
    }

}
