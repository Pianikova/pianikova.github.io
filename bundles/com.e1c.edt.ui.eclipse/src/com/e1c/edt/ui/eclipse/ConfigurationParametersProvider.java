/**
 *
 */
package com.e1c.edt.ui.eclipse;

import java.util.Optional;

import com.e1c.edt.ai.IConfigurationParametersProvider;
import com.e1c.edt.ai.assistent.model.ProjectId;
import com.e1c.edt.ai.assistent.model.ConfigurationParameters;

public class ConfigurationParametersProvider
    implements IConfigurationParametersProvider
{
    @Override
    public Optional<ConfigurationParameters> getParameters(ProjectId projectId)
    {
        return Optional.empty();
    }
}
