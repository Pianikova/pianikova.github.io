/**
 *
 */
package com.e1c.edt.semantic;

import java.net.URL;
import java.time.Duration;
import java.util.Optional;

import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.ParametersParser;
import com.e1c.edt.ai.assistent.model.CodeCompletionPolicy;
import com.e1c.edt.ai.assistent.model.Parameters;
import com.e1c.edt.ai.assistent.model.ProjectId;
import com.e1c.edt.ai.assistent.model.Verbosity;

class Settings
    implements ISettings
{
    @Override
    public boolean isEnabled()
    {
        return true;
    }

    @Override
    public boolean isStatusBarVisible()
    {
        return true;
    }

    @Override
    public boolean isActivationInfoVisible()
    {
        return false;
    }

    @Override
    public boolean isAutoOpenDiffPreview()
    {
        return true;
    }

    @Override
    public boolean isBackgroundAnalysisEnabled()
    {
        // Headless/semantic runtime has no editors or save events to drive background analysis.
        return false;
    }

    @Override
    public boolean hasClientToken()
    {
        return true;
    }

    @Override
    public String getClientToken()
    {
        return "semantic"; //$NON-NLS-1$
    }

    @Override
    public String getClientUniqueId()
    {
        return "semantic"; //$NON-NLS-1$
    }

    @Override
    public Optional<String> getInstanceType()
    {
        return Optional.empty();
    }

    @Override
    public CodeCompletionPolicy getCodeCompletionPolicy()
    {
        return CodeCompletionPolicy.INTENSVE;
    }

    @Override
    public int getTabWidth()
    {
        return 4;
    }

    @Override
    public int getCodeCompletionLinesCount()
    {
        return 5;
    }

    @Override
    public Duration getMinRequestDelay()
    {
        return Duration.ofMillis(ParametersParser.DEFAULT_MIN_DELAY);
    }

    @Override
    public Duration getTimeout()
    {
        return Duration.ofMillis(ParametersParser.DEAULT_TIMEOUT);
    }

    @Override
    public String getLineSeparator()
    {
        return System.lineSeparator();
    }

    @Override
    public int getPrefixLength(ProjectId projectId)
    {
        return ParametersParser.DEFAULT_PREFIX_LEN;
    }

    @Override
    public int getSuffixLength(ProjectId projectId)
    {
        return ParametersParser.DEFAULT_SUFFIX_LEN;
    }

    @Override
    public boolean isExperimental()
    {
        return true;
    }

    @Override
    public boolean sendGlobalContext(ProjectId projectId)
    {
        return false;
    }

    @Override
    public String getLanguage()
    {
        return "Russian"; //$NON-NLS-1$
    }

    @Override
    public String getTheme()
    {
        return "Default"; //$NON-NLS-1$
    }

    @Override
    public Verbosity getVerbosity()
    {
        return ParametersParser.DEFAULT_VERBOSITY;
    }

    @Override
    public Optional<String> getResources()
    {
        // TODO Auto-generated method stub
        return Optional.empty();
    }

    @Override
    public int getGitDiffContextLines(ProjectId projectId)
    {
        return ParametersParser.DEFAULT_GIT_CONTEXT_LINES;
    }

    @Override
    public URL getUrl()
    {
        return null;
    }

    @Override
    public URL getChatUrl()
    {
        return null;
    }

    @Override
    public String getHomePage()
    {
        return null;
    }

    @Override
    public String getUpdateUrl()
    {
        return null;
    }

    @Override
    public String getPluginFeature()
    {
        return null;
    }

    @Override
    public Parameters getUserParameters()
    {
        return new Parameters();
    }
}
