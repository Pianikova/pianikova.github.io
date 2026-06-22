/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context.tools;

import java.util.List;

import org.eclipse.core.resources.IProject;

import com._1c.g5.v8.dt.core.platform.IDerivedDataManagerProvider;
import com._1c.g5.v8.dt.validation.marker.v2.IMarkerManagerV2;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IProjectBuilder;
import com.e1c.edt.ai.ISettings;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

/**
 * 1C:EDT project builder. In EDT the Eclipse incremental build is intentionally skipped; project
 * validation is produced asynchronously by the Derived Data (DD) pipeline and the resulting markers
 * are flushed to the marker store by a periodic committer. So instead of building, this waits for
 * the validation DD segments to settle and force-flushes pending markers, so a subsequent marker
 * read returns the full set rather than a partial snapshot.
 */
public class ProjectBuilder
    implements IProjectBuilder
{
    /**
     * IDs of the DD segments that produce/clear validation markers (the AFTER_BUILD/BEFORE_BUILD
     * check segments + marker cleaner). We wait only for these instead of the whole DD pipeline
     * (forms, BSL indexing, references, ...) so marker reads return as soon as validation is done.
     * <p>
     * Source: {@code com.e1c.g5.v8.dt.internal.check.derived.CheckDerivedDataContributor} (internal,
     * not API — hence duplicated here). If a future EDT renames these, the wait degrades to a fast
     * no-op (correctness falls back to the periodic committer), it does not slow down.
     */
    private static final String[] CHECK_SEGMENTS = { "CDI_CHECKS_SEGMENT", //$NON-NLS-1$
        "M_CHECKS_SEGMENT", //$NON-NLS-1$
        "CM_CHECKS_SEGMENT", //$NON-NLS-1$
        "L_CHECKS_SEGMENT", //$NON-NLS-1$
        "CL_CHECKS_SEGMENT", //$NON-NLS-1$
        "M_CLEANER_SEGMENT" }; //$NON-NLS-1$

    private final IDerivedDataManagerProvider derivedDataManagerProvider;
    private final IMarkerManagerV2 markerManager;
    private final ISettings settings;

    @Inject
    public ProjectBuilder(IDerivedDataManagerProvider derivedDataManagerProvider,
        IMarkerManagerV2 markerManager, ISettings settings)
    {
        Preconditions.checkNotNull(derivedDataManagerProvider);
        Preconditions.checkNotNull(markerManager);
        Preconditions.checkNotNull(settings);

        this.derivedDataManagerProvider = derivedDataManagerProvider;
        this.markerManager = markerManager;
        this.settings = settings;
    }

    @Override
    public boolean build(IProject project, ICancellationToken cancellationToken)
    {
        if (cancellationToken.isCanceled())
        {
            return false;
        }

        boolean complete = false;
        try
        {
            var derivedDataManager = derivedDataManagerProvider.get(project);
            if (derivedDataManager != null)
            {
                var timeout = settings.getTimeout();
                long timeoutMillis = timeout != null ? timeout.toMillis() : -1L;
                // Wait only for the validation/marker DD segments. Returns immediately when they are
                // already computed (fast path), and does not block on unrelated DD (forms, indexing).
                complete = derivedDataManager.waitComputation(timeoutMillis, false, CHECK_SEGMENTS);
            }
            else
            {
                // No DD manager (e.g. a non-1C project): nothing to wait for, treat as complete.
                complete = true;
            }
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            return false;
        }
        catch (Exception e)
        {
            // Unable to query DD state; fall back to flushing/reading whatever is committed.
            complete = false;
        }

        // Flush the periodic marker committer so freshly produced markers are visible to readers,
        // even when validation did not fully settle within the timeout.
        try
        {
            markerManager.commit(List.of(project));
        }
        catch (Exception e)
        {
            //
        }

        return complete && !cancellationToken.isCanceled();
    }
}
