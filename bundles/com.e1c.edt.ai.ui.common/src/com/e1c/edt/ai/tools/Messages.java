/**
 *
 */
package com.e1c.edt.ai.tools;

import org.eclipse.osgi.util.NLS;

public class Messages
    extends NLS
{
    private static final String BUNDLE_NAME = Messages.class.getPackageName() + ".messages"; //$NON-NLS-1$

    // Status indicators
    public static String ToolNameTemplate;
    public static String ToolFailed;

    // Project operations
    public static String ProjectsTitle;
    public static String ProjectsLoadedTemplate;
    public static String ProjectsDetailsTitle;

    // File operations
    public static String ReadTitleTemplate;
    public static String ReadTemplate;
    public static String WriteTitleTemplate;
    public static String WrittenTemplate;
    public static String WriteDetailsSummary;
    public static String DeleteTitleTemplate;
    public static String DeletedTemplate;
    public static String Deleted;
    public static String EditTitleTemplate;
    public static String EditedTemplate;
    public static String EditDetailsSummary;

    // Search operations
    public static String SearchTitleTemplate;
    public static String FindTemplate;
    public static String SearchResults;
    public static String Matches;
    public static String Line;
    public static String FindFilesTitleTemplate;
    public static String FilesFoundTemplate;
    public static String FileSize;
    public static String SearchQuery;
    public static String FileNamePatterns;
    public static String AllFiles;
    public static String ProjectName;
    public static String MaxCommits;
    public static String CurrentProject;
    public static String SelectedFile;
    public static String GlobTitleTemplate;
    public static String GlobTemplate;
    public static String GlobPattern;
    public static String GlobPath;
    public static String GlobDepth;
    public static String GlobFilesFound;
    public static String GlobDirectoriesFound;
    public static String ListTitleTemplate;
    public static String ListTemplate;
    public static String GitCommitsTitleTemplate;
    public static String GitCommitsFoundTemplate;
    public static String CommitsList;
    public static String Author;
    public static String Date;
    public static String ChangedFiles;
    public static String Files;
    public static String GitDiffTitleTemplate;
    public static String GitDiffFoundTemplate;
    public static String GitUncommittedDiffTemplate;
    public static String GitCommitDiffTemplate;
    public static String NoGitChangesTemplate;
    public static String NoGitUncommittedChangesTemplate;
    public static String NoGitCommitChangesTemplate;
    public static String LocalChangesTitleTemplate;
    public static String LocalChangesFoundTemplate;
    public static String NoLocalChangesFoundTemplate;
    public static String LocalHistoryTitleTemplate;
    public static String LocalHistoryFoundTemplate;
    public static String ViewHistory;
    public static String NoLocalHistoryFound;
    public static String NavigationHistoryTitleTemplate;
    public static String NavigationHistoryFoundTemplate;
    public static String ViewNavigationHistory;
    public static String NoNavigationHistoryFound;
    public static String Location;
    public static String Current;

    // Command operations
    public static String ExecuteTitleTemplate;
    public static String ExecutedTemplate;
    public static String CommandCategoriesTitle;
    public static String CommandCategoriesLoadedTemplate;
    public static String CommandsTitle;
    public static String CommandsLoadedTemplate;
    public static String ExecuteCommandTitle;

    // JShell operations
    public static String JShellExecutingTemplate;
    public static String JShellExecutedTemplate;
    public static String JShellErrorTemplate;
    public static String JShellSessionCreated;
    public static String JShellSessionCodeHistory;
    public static String JShellAvailableBindings;
    public static String JShellSessionCreating;

    // Marker operations
    public static String MarkersTitle;
    public static String MarkersLoadedTemplate;
    public static String CreateMarkersTitle;
    public static String MarkersCreatedMessage;
    public static String RemoveMarkersTitle;
    public static String MarkersRemovedMessage;
    public static String CreateMarkersTitleTemplate;
    public static String RemoveMarkersTitleTemplate;

    // Execute tool details
    public static String ExecutionDetails;
    public static String ExitCode;
    public static String StdOutLabel;
    public static String StdErrLabel;
    public static String OutputTruncatedLines;

    // Error details
    public static String ErrorDetails;
    public static String ErrorArguments;
    public static String ErrorContent;

    static
    {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages()
    {
    }
}





