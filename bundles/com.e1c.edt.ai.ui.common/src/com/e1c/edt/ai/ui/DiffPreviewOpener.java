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
            log.trace(TracingSources.CHAT, AI_CHAT, () -> "autoOpenDiff skipped, token: " + token //$NON-NLS-1$
                + ", closed: " + closedTokens.contains(token)); //$NON-NLS-1$
            return;
        }

        var input = buildInput(token);
        if (input != null)
        {
            // Open without taking focus, so the user stays in the chat.
            log.trace(TracingSources.CHAT, AI_CHAT, () -> "autoOpenDiff opening, token: " + token); //$NON-NLS-1$
            CompareUI.openCompareEditor(input, false);

            // openCompareEditor spins a progress event loop, during which a racing closeDiff (auto-
            // confirm mode) may have run before this editor was registered and thus found nothing.
            // Re-check and close now that the editor exists, so the preview never lingers.
            if (closedTokens.contains(token))
            {
                log.trace(TracingSources.CHAT, AI_CHAT,
                    () -> "autoOpenDiff: token closed during open, closing now: " + token); //$NON-NLS-1$
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
        log.trace(TracingSources.CHAT, AI_CHAT,
            () -> "closeDiff token: " + token + ", editor found: " + (ref != null)); //$NON-NLS-1$ //$NON-NLS-2$
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

        // The element name is the human-friendly breadcrumb; the type (for syntax coloring) is
        // derived from the real file name's extension.
        var realFileName = new java.io.File(preview.getFilePath()).getName();
        var left = new StringTypedElement(preview.getDisplayName(), realFileName, preview.getOriginalContent());
        var right = new StringTypedElement(preview.getDisplayName(), realFileName, preview.getProposedContent());

        var configuration = new CompareConfiguration();
        configuration.setLeftEditable(false);
        configuration.setRightEditable(false);
        configuration.setLeftLabel(MessageFormat.format(Messages.DiffCompareCurrentLabel, preview.getFilePath()));
        configuration.setRightLabel(Messages.DiffCompareProposedLabel);

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
                    log.trace(TracingSources.CHAT, AI_CHAT, () -> "findOpenEditorRef scan ref id: " + ref.getId()); //$NON-NLS-1$
                    if (!COMPARE_EDITOR_ID.equals(ref.getId()))
                    {
                        continue;
                    }
                    try
                    {
                        var editorInput = ref.getEditorInput();
                        log.trace(TracingSources.CHAT, AI_CHAT, () -> "findOpenEditorRef compare input: " //$NON-NLS-1$
                            + editorInput.getClass().getName());
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
