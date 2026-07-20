/**
 *
 */
package com.e1c.edt.ai.assistent;

import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import com.e1c.edt.ai.ICancellationToken;
import org.eclipse.core.resources.IProject;
import com.e1c.edt.ai.assistent.model.Session;

public interface ISessionCall
{
    <T> CompletableFuture<HttpResponse<T>> call(IProject project, ICancellationToken cancellationToken,
        Function<Optional<Session>, CompletableFuture<HttpResponse<T>>> taskSupplier);
}
