/**
 *
 */
package com.e1c.edt.ai;

import java.net.URL;
import java.time.Duration;
import java.util.Optional;

import com.e1c.edt.ai.assistent.model.CodeCompletionPolicy;
import com.e1c.edt.ai.assistent.model.Parameters;
import com.e1c.edt.ai.assistent.model.ProjectId;
import com.e1c.edt.ai.assistent.model.Verbosity;

public interface ISettings
{
    public static final String LANGUAGE_RUSSIAN = "Russian"; //$NON-NLS-1$
    public static final String LANGUAGE_ENGLISH = "English"; //$NON-NLS-1$

    boolean isEnabled();

    boolean isStatusBarVisible();

    boolean isAutoOpenDiffPreview();

    boolean hasClientToken();

    String getClientToken();

    String getClientUniqueId();

    Optional<String> getInstanceType();

    CodeCompletionPolicy getCodeCompletionPolicy();

    int getTabWidth();

    int getCodeCompletionLinesCount();

    Duration getMinRequestDelay();

    Duration getTimeout();

    String getLineSeparator();

    int getPrefixLength(ProjectId projectId);

    int getSuffixLength(ProjectId projectId);

    boolean isExperimental();

    boolean sendGlobalContext(ProjectId projectId);

    String getLanguage();

    String getTheme();

    Verbosity getVerbosity();

    Optional<String> getResources();

    int getGitDiffContextLines(ProjectId projectId);

    URL getUrl();

    URL getChatUrl();

    String getHomePage();

    String getUpdateUrl();

    String getPluginFeature();

    Parameters getUserParameters();
}
