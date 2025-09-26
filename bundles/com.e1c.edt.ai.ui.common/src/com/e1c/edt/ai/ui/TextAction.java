/**
 *
 */
package com.e1c.edt.ai.ui;

public enum TextAction
{
    // Suggest your option
    SUGGEST_YOUR_OPTION(Messages.SuggestYourOption, Images.SUGGEST_YOUR_OPTION, IResourceProvider.SUGGEST_YOUR_OPTION),

    // Correct errors
    CORRECT_ERRORS(Messages.CorrectErrors, Images.CORRECT_ERRORS, IResourceProvider.CORRECT_ERRORS),

    // In other words
    IN_OTHER_WORDS(Messages.InOtherWords, Images.IN_OTHER_WORDS, IResourceProvider.IN_OTHER_WORDS),

    // Improve style
    IMPROVE_STYLE(Messages.ImproveStyle, Images.IMPROVE_STYLE, IResourceProvider.IMPROVE_STYLE);

    public final String title;
    public final String imageName;
    public final String resourceName;

    TextAction(String title, String imageName, String resourceName)
    {
        this.title = title;
        this.imageName = imageName;
        this.resourceName = resourceName;
    }
}
