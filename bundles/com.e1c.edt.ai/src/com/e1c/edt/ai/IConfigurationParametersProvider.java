/**
 *
 */
package com.e1c.edt.ai;

import java.util.Optional;

import com.e1c.edt.ai.assistent.model.ProjectId;
import com.e1c.edt.ai.assistent.model.ConfigurationParameters;

public interface IConfigurationParametersProvider
{
    Optional<ConfigurationParameters> getParameters(ProjectId projectId);
}
