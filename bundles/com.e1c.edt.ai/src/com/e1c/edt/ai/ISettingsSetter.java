/**
 *
 */
package com.e1c.edt.ai;

import com.e1c.edt.ai.assistent.model.CodeCompletionPolicy;
import com.e1c.edt.ai.assistent.model.Parameters;
import org.eclipse.core.resources.IProject;

public interface ISettingsSetter
{
    void applySessionParameters(IProject project, Parameters sessionParameters);

    void applyGlobalSessionParameters(Parameters sessionParameters);

    void setCodeCompletionPolicy(CodeCompletionPolicy codeCompletionPolicy);

    void setStatusBarVisible(boolean visible);

    void setActivationInfoVisible(boolean visible);
}
