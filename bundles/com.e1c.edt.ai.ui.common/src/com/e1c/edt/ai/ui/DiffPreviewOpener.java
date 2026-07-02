/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareUI;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.TracingSources;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

/**
 * Default {@link IDiffPreviewOpener}: opens a standard read-only Eclipse compare editor (current vs
 * proposed content) using the in-memory snapshot stored at RENDER time.
 * <p>
 * To avoid retaining closed editors, no references to {@link DiffCompareEditorInput} or
 * {@link IWorkbenchPage} are kept. Editors are located live by their {@link DiffCompareEditorInput}
 * token. The only retained state is two small sets of token strings.
 */
public class DiffPreviewOpener
    implements IDiffPreviewOpener
{
    private static final String AI_CHAT = "AI Chat"; //$NON-NLS-1$
    private static final String COMPARE_EDITOR_ID = "org.eclipse.compare.CompareEditor"; //$NON-NLS-1$

    private final IDiffPreviewStore store;
    private final ILog log;

    // Tokens already auto-opened this session: prevents repeated RENDER passes from re-opening (or
    // re-opening a tab the user closed). Holds only short id strings — no editor/page references.
    private final Set<String> autoOpenedTokens = Collections.newSetFromMap(new ConcurrentHashMap<>());

    // Tokens whose edit has been applied (closeDiff called). In auto-confirm mode RENDER (open) and
    // CALL (close) race; if close runs before open, this set makes the later autoOpenDiff a no-op so
    // the preview tab never lingers. Manual openDiff (link click) ignores this set.
    private final Set<String> closedTokens = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @Inject
    public DiffPreviewOpener(IDiffPreviewStore store, ILog log)
    {
        Preconditions.checkNotNull(store);
        Preconditions.checkNotNull(log);
        this.store = store;
        this.log = log;
    }

    @Override
    public void openDiff(String token)
    {
        var existing = findOpenEditorRef(token);
        if (existing != null)
        {
            var part = existing.getEditor(true);
            if (part != null)
            {
                existing.getPage().activate(part);
            }
            return;
        }

        var input = buildInput(token);
        if (input != null)
        {
            CompareUI.openCompareEditor(input, true);
        }
    }

    @Override
    public void autoOpenDiff(String token)
    {
        if (token == null || token.isBlank() || closedTokens.contains(token) || !autoOpenedTokens.add(token))
        {
            return;
        }

        var input = buildInput(token);
        if (input != null)
        {
            // Open without taking focus, so the user stays in the chat.
            CompareUI.openCompareEditor(input, false);

            // openCompareEditor spins a progress event loop, during which a racing closeDiff (auto-
            // confirm mode) may have run before this editor was registered and thus found nothing.
            // Re-check and close now that the editor exists, so the preview never lingers.
            if (closedTokens.contains(token))
            {
                closeDiff(token);
            }
        }
    }

    @Override
    public void closeDiff(String token)
    {
        if (token == null || token.isBlank())
        {
            return;
        }

        // Mark finalized first, so a racing autoOpenDiff (close-before-open in auto-confirm mode)
        // becomes a no-op instead of leaving an orphan preview tab.
        closedTokens.add(token);

        var ref = findOpenEditorRef(token);
        if (ref != null)
        {
            ref.getPage().closeEditors(new IEditorReference[] { ref }, false);
        }
    }

    /**
     * Builds a compare-editor input from the stored snapshot, or {@code null} if none is registered.
     */
    private DiffCompareEditorInput buildInput(String token)
    {
        var optionalPreview = store.get(token);
        if (optionalPreview.isEmpty())
        {
            log.trace(TracingSources.CHAT, AI_CHAT, () -> "No diff preview registered for token: " + token); //$NON-NLS-1$
            return null;
        }

        var preview = optionalPreview.get();

        // Proposed on the LEFT, current on the RIGHT. The BSL/compare merge viewer colors changes by
        // position (left block green, right block red) and ignores programmatic color overrides, so
        // this orientation yields the git convention: additions (proposed, left) green, deletions
        // (current, right) red. Element type is the file extension, keeping BSL syntax highlighting.
        var realFileName = new java.io.File(preview.getFilePath()).getName();
        var left = new StringTypedElement(preview.getDisplayName(), realFileName, preview.getProposedContent());
        var right = new StringTypedElement(preview.getDisplayName(), realFileName, preview.getOriginalContent());

        var configuration = new CompareConfiguration();
        configuration.setLeftEditable(false);
        configuration.setRightEditable(false);
        configuration.setLeftLabel(Messages.DiffCompareProposedLabel);
        configuration.setRightLabel(MessageFormat.format(Messages.DiffCompareCurrentLabel, preview.getFilePath()));
        // Swap the panes by default, as if the compare view's "Swap Left and Right" button were
        // pressed: the current file is shown on the left and the proposed changes on the right.
        configuration.setProperty(CompareConfiguration.MIRRORED, Boolean.TRUE);

        var input = new DiffCompareEditorInput(left, right, configuration, token);
        input.setTitle(preview.getDisplayName());
        return input;
    }

    /**
     * Locates the open compare-editor reference previously opened for the given token, searching all
     * workbench windows/pages. Returns {@code null} if none is open. Matches by editor input without
     * forcing the editor part to be materialized.
     */
    private IEditorReference findOpenEditorRef(String token)
    {
        if (token == null || token.isBlank())
        {
            return null;
        }

        var workbench = PlatformUI.getWorkbench();
        if (workbench == null)
        {
            return null;
        }

        for (var window : workbench.getWorkbenchWindows())
        {
            for (IWorkbenchPage page : window.getPages())
            {
                for (IEditorReference ref : page.getEditorReferences())
                {
                    if (!COMPARE_EDITOR_ID.equals(ref.getId()))
                    {
                        continue;
                    }
                    try
                    {
                        var editorInput = ref.getEditorInput();
                        if (editorInput instanceof DiffCompareEditorInput
                            && token.equals(((DiffCompareEditorInput)editorInput).getToken()))
                        {
                            return ref;
                        }
                    }
                    catch (PartInitException e)
                    {
                        // Input could not be restored (e.g. an unrelated editor from a previous
                        // session) — not one of ours; skip it.
                    }
                }
            }
        }
        return null;
    }
}
