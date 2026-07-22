/*
 * Copyright (C) 2026, 1C
 */
package com.e1c.edt.ai.context.tools.metadata;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.ToolException;

public class MetadataMutationServiceTest
{
    @Test
    public void shouldSerializeBmMutations()
        throws ClassNotFoundException
    {
        var service = Class.forName("com.e1c.edt.ai.context.tools.metadata.MetadataMutationService"); //$NON-NLS-1$
        var execute = java.util.Arrays.stream(service.getDeclaredMethods())
            .filter(method -> "execute".equals(method.getName())) //$NON-NLS-1$
            .findFirst()
            .orElseThrow(AssertionError::new);

        Assert.assertTrue(Modifier.isSynchronized(execute.getModifiers()));
    }

    @Test
    public void shouldBuildTopObjectResourcePathForChildTarget()
        throws ReflectiveOperationException
    {
        var service = Class.forName("com.e1c.edt.ai.context.tools.metadata.MetadataMutationService"); //$NON-NLS-1$
        var method = service.getDeclaredMethod("metadataRelativePath", String.class); //$NON-NLS-1$
        method.setAccessible(true);

        var path = method.invoke(null, "Document.Order.Lines.Quantity"); //$NON-NLS-1$

        Assert.assertEquals("src/Documents/Order/Order.mdo", path); //$NON-NLS-1$
    }

    @Test
    public void shouldRejectCanceledOperationBeforeMutation()
        throws ReflectiveOperationException
    {
        var service = Class.forName("com.e1c.edt.ai.context.tools.metadata.MetadataMutationService"); //$NON-NLS-1$
        var method = service.getDeclaredMethod("checkCanceled", ICancellationToken.class); //$NON-NLS-1$
        method.setAccessible(true);
        var token = Mockito.mock(ICancellationToken.class);
        Mockito.when(token.isCanceled()).thenReturn(true);

        try
        {
            method.invoke(null, token);
            Assert.fail("Cancellation must stop the mutation."); //$NON-NLS-1$
        }
        catch (InvocationTargetException e)
        {
            Assert.assertTrue(e.getCause() instanceof ToolException);
        }
    }

    @Test
    public void shouldRejectDeleteWhenMetadataResourceRemains()
        throws ReflectiveOperationException, java.io.IOException
    {
        var service = Class.forName("com.e1c.edt.ai.context.tools.metadata.MetadataMutationService"); //$NON-NLS-1$
        var requestType = Class.forName("com.e1c.edt.ai.context.tools.metadata.MetadataRequest"); //$NON-NLS-1$
        var responseType = Class.forName("com.e1c.edt.ai.context.tools.metadata.MetadataResponse"); //$NON-NLS-1$
        var request = newInstance(requestType);
        var response = newInstance(responseType);
        setField(requestType, request, "operation", "removeObject"); //$NON-NLS-1$ //$NON-NLS-2$
        var resource = Files.createTempFile("edt-metadata-delete-check", ".mdo"); //$NON-NLS-1$ //$NON-NLS-2$
        setField(responseType, response, "resourcePath", resource.toString()); //$NON-NLS-1$
        var method = service.getDeclaredMethod("verifyResourceState", requestType, responseType); //$NON-NLS-1$
        method.setAccessible(true);

        try
        {
            method.invoke(null, request, response);
            Assert.fail("A stale metadata resource must make deletion fail."); //$NON-NLS-1$
        }
        catch (InvocationTargetException e)
        {
            Assert.assertTrue(e.getCause() instanceof ToolException);
        }
        finally
        {
            Files.deleteIfExists(resource);
        }
    }

    @Test
    public void shouldWaitForCreatedMetadataResource()
        throws ReflectiveOperationException, java.io.IOException, InterruptedException
    {
        var service = Class.forName("com.e1c.edt.ai.context.tools.metadata.MetadataMutationService"); //$NON-NLS-1$
        var requestType = Class.forName("com.e1c.edt.ai.context.tools.metadata.MetadataRequest"); //$NON-NLS-1$
        var responseType = Class.forName("com.e1c.edt.ai.context.tools.metadata.MetadataResponse"); //$NON-NLS-1$
        var request = newInstance(requestType);
        var response = newInstance(responseType);
        setField(requestType, request, "operation", "createObject"); //$NON-NLS-1$ //$NON-NLS-2$
        var folder = Files.createTempDirectory("edt-metadata-create-check"); //$NON-NLS-1$
        var resource = folder.resolve("Object.mdo"); //$NON-NLS-1$
        setField(responseType, response, "resourcePath", resource.toString()); //$NON-NLS-1$
        var token = Mockito.mock(ICancellationToken.class);
        var method = service.getDeclaredMethod("awaitResourceState", requestType, responseType, //$NON-NLS-1$
            ICancellationToken.class, long.class);
        method.setAccessible(true);
        var writer = new Thread(() -> {
            try
            {
                Thread.sleep(100L);
                Files.createFile(resource);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
            catch (java.io.IOException e)
            {
                throw new AssertionError(e);
            }
        });

        try
        {
            writer.start();
            method.invoke(null, request, response, token, 2_000L);
            writer.join();
            Assert.assertTrue(Files.exists(resource));
        }
        finally
        {
            Files.deleteIfExists(resource);
            Files.deleteIfExists(folder);
        }
    }

    private static Object newInstance(Class<?> type)
        throws ReflectiveOperationException
    {
        var constructor = type.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    private static void setField(Class<?> type, Object instance, String name, Object value)
        throws ReflectiveOperationException
    {
        var field = type.getDeclaredField(name);
        field.setAccessible(true);
        field.set(instance, value);
    }
}
