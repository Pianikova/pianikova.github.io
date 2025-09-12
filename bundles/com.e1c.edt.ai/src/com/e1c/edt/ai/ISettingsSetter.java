/**
 *
 */
package com.e1c.edt.ai;

import com.e1c.edt.ai.assistent.model.CodeCompletionPolicy;
import com.e1c.edt.ai.assistent.model.Parameters;
import com.e1c.edt.ai.assistent.model.ProjectId;

public interface ISettingsSetter
{
    void applySessionParameters(ProjectId projectId, Parameters sessionParameters);

    void setCodeCompletionPolicy(CodeCompletionPolicy codeCompletionPolicy);
}
