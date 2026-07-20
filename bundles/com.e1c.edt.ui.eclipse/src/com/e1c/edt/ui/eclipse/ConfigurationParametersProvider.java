/**
 *
 */
package com.e1c.edt.ui.eclipse;

import java.util.Optional;

import org.eclipse.core.resources.IProject;

import com.e1c.edt.ai.IConfigurationParametersProvider;
import com.e1c.edt.ai.assistent.model.ConfigurationParameters;

public class ConfigurationParametersProvider
    implements IConfigurationParametersProvider
{
    @Override
    public Optional<ConfigurationParameters> getParameters(IProject project)
    {
        return Optional.empty();
    }
}
