/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context.tools.metadata;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;

import com._1c.g5.v8.bm.integration.IBmModel;
import com._1c.g5.v8.dt.core.platform.IBmModelManager;
import com._1c.g5.v8.dt.form.mapping.cmi.FormCommandInterfaceMapping;
import com._1c.g5.v8.dt.form.mapping.core.Mapping;
import com._1c.g5.v8.dt.form.mapping.independent.FormIndependentCommandMapping;
import com._1c.g5.v8.dt.form.mapping.item.FormItemsMapping;
import com._1c.g5.v8.dt.form.mapping.parameterized.FormParameterizedCommandMapping;
import com._1c.g5.v8.dt.form.model.Form;
import com.e1c.edt.ai.ui.BaseActivator;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Drives the same headless form-loading machinery EDT's own form editor uses (the
 * {@code Mapping<TRootModel, Form>} family: command interface, item tree, and parameterized/independent
 * commands) against a form this tool just created or changed, and reports whatever it fails on.
 * <p>
 * This exists because a form can pass every check this tool otherwise runs — 1C's own marker checker
 * ({@code MdValidationChecker}) and this tool's own {@link MetadataStructuralValidator} whitelist — and
 * still crash the first EDT UI that opens it, because neither check exercises the actual loading code
 * path. Rather than hand-writing one check per class of defect (the approach that produced false
 * positives on {@code Visible.userVisible} before {@link MetadataStructuralValidator} was narrowed to a
 * whitelist), this calls EDT's own loader and treats any exception it raises as ground truth — it
 * generalizes to defects nobody has hand-coded a check for yet.
 * <p>
 * Every {@code Mapping} subclass here has {@code @Inject} fields its own public constructors leave
 * unset — they are meant to be built by EDT's own Guice injector, never by a bare {@code new}. Each
 * instance is field-injected via {@link BaseActivator#injectMembers} right after construction, exactly
 * like every other object this codebase hands to an Eclipse extension point.
 * <p>
 * Which constructor overload each class exposes has already been observed to differ between EDT
 * platform versions this tool compiles against and the one actually running (an older
 * {@code FormCommandInterfaceMapping(Supplier<Form>)} calling convention raised
 * {@code NoSuchMethodError} against a newer runtime that only kept the 2-arg overload). {@link #construct}
 * therefore picks a constructor reflectively from whichever overloads the runtime class actually
 * declares, rather than calling one fixed at compile time.
 * <p>
 * {@code Mapping.buildRootModel()} schedules an asynchronous {@link Job} and keeps it in a private field
 * with no public accessor for its outcome, so this reflects into that field to call the ordinary
 * {@link Job#join()} / {@link Job#getResult()}. <b>Call this only after the write transaction that
 * produced the form has committed</b>: the job opens its own read-only BM transaction on a worker
 * thread, and joining it from the calling thread while that thread still holds an open write transaction
 * on the same model can deadlock the two against each other.
 */
@Singleton
final class FormLoadValidator
{
    private final IBmModelManager modelManager;

    @Inject
    FormLoadValidator(IBmModelManager modelManager)
    {
        this.modelManager = modelManager;
    }

    /**
     * @param form the form this tool just created or changed; never {@code null}
     * @return one message per {@code Mapping} subclass EDT's own loader failed to build or construct;
     *         empty when the form loads cleanly
     */
    List<String> findLoadErrors(Form form)
    {
        var problems = new ArrayList<String>();
        var bmModel = modelManager.getModel(form);
        Supplier<Form> input = () -> form;
        run(problems, "command interface", FormCommandInterfaceMapping.class, form, input, bmModel); //$NON-NLS-1$
        run(problems, "item tree", FormItemsMapping.class, form, input, bmModel); //$NON-NLS-1$
        run(problems, "parameterized commands", FormParameterizedCommandMapping.class, form, input, bmModel); //$NON-NLS-1$
        run(problems, "independent commands", FormIndependentCommandMapping.class, form, input, bmModel); //$NON-NLS-1$
        return problems;
    }

    private void run(List<String> problems, String label, Class<? extends Mapping<?, Form>> type, Form form,
        Supplier<Form> input, IBmModel bmModel)
    {
        Mapping<?, Form> mapping;
        try
        {
            mapping = construct(type, form, input, bmModel);
        }
        catch (ReflectiveOperationException e)
        {
            // A version skew this tool has no constructor shape for; report it like any other failure
            // rather than letting it abort the whole mutation response.
            problems.add(label + ": could not build " + type.getSimpleName() + " on this EDT version: " + e); //$NON-NLS-1$ //$NON-NLS-2$
            return;
        }
        BaseActivator.injectMembers(mapping);
        try
        {
            mapping.buildRootModel();
            var job = job(mapping);
            if (job == null)
            {
                return;
            }
            job.join();
            var result = job.getResult();
            if (result != null && result.getSeverity() >= IStatus.ERROR)
            {
                var exception = result.getException();
                problems.add(label + ": " + result.getMessage() //$NON-NLS-1$
                    + (exception == null ? "" : " (" + exception + ")")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
        finally
        {
            mapping.dispose();
        }
    }

    /**
     * Picks whichever declared constructor of {@code type} can be satisfied entirely from
     * {@code form}/{@code input}/{@code bmModel}/this validator's own {@code modelManager}, preferring
     * the one with the most parameters (the more the constructor sets directly, the less this depends on
     * {@code @Inject} field bindings being present). Every {@code Mapping<?, Form>} subclass this tool
     * uses only ever declares constructors shaped from that same small set of types, across every
     * observed platform version — just not the same overloads from version to version.
     */
    @SuppressWarnings("unchecked")
    private Mapping<?, Form> construct(Class<? extends Mapping<?, Form>> type, Form form, Supplier<Form> input,
        IBmModel bmModel) throws ReflectiveOperationException
    {
        var constructors = type.getDeclaredConstructors();
        java.util.Arrays.sort(constructors, Comparator.comparingInt(Constructor<?>::getParameterCount).reversed());
        for (var constructor : constructors)
        {
            var parameterTypes = constructor.getParameterTypes();
            var args = new Object[parameterTypes.length];
            var matched = true;
            for (int i = 0; i < parameterTypes.length; i++)
            {
                if (parameterTypes[i] == Supplier.class)
                {
                    args[i] = input;
                }
                else if (parameterTypes[i] == Form.class)
                {
                    args[i] = form;
                }
                else if (parameterTypes[i] == IBmModel.class)
                {
                    args[i] = bmModel;
                }
                else if (parameterTypes[i] == IBmModelManager.class)
                {
                    args[i] = modelManager;
                }
                else
                {
                    matched = false;
                    break;
                }
            }
            if (matched)
            {
                constructor.setAccessible(true);
                return (Mapping<?, Form>)constructor.newInstance(args);
            }
        }
        throw new NoSuchMethodException(type.getName() + " has no constructor this tool knows how to satisfy"); //$NON-NLS-1$
    }

    private static Job job(Mapping<?, Form> mapping)
    {
        var field = MetadataMutationService.findField(Mapping.class, "job"); //$NON-NLS-1$
        if (field == null)
        {
            return null;
        }
        field.setAccessible(true);
        try
        {
            return (Job)field.get(mapping);
        }
        catch (IllegalAccessException e)
        {
            return null;
        }
    }
}
