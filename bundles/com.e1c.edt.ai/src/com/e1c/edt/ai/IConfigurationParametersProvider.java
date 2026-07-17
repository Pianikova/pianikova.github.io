/**
 *
 */
package com.e1c.edt.ai;

import java.util.Optional;

import org.eclipse.core.resources.IProject;
import com.e1c.edt.ai.assistent.model.ConfigurationParameters;

public interface IConfigurationParametersProvider
{
    Optional<ConfigurationParameters> getParameters(IProject project);
}
