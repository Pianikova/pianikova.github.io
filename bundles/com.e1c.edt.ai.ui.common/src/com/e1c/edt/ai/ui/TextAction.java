/**
 *
 */
package com.e1c.edt.ai.ui;

public enum TextAction
{
    // Suggest your option
    SUGGEST_YOUR_OPTION(Messages.SuggestYourOption, Images.SUGGEST_YOUR_OPTION, "text-suggest"), //$NON-NLS-1$

    // Correct errors
    CORRECT_ERRORS(Messages.CorrectErrors, Images.CORRECT_ERRORS, "text-correct-errors"), //$NON-NLS-1$

    // In other words
    IN_OTHER_WORDS(Messages.InOtherWords, Images.IN_OTHER_WORDS, "text-in-other-words"), //$NON-NLS-1$

    // Improve style
    IMPROVE_STYLE(Messages.ImproveStyle, Images.IMPROVE_STYLE, "text-improve-style"); //$NON-NLS-1$

    public final String title;
    public final String imageName;
    public final String skillId;

    TextAction(String title, String imageName, String skillId)
    {
        this.title = title;
        this.imageName = imageName;
        this.skillId = skillId;
    }
}
