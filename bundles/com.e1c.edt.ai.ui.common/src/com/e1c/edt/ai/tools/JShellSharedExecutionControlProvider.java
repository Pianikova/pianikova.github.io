/**
 *
 */
package com.e1c.edt.ai.tools;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.base.Preconditions;

import jdk.jshell.execution.LoaderDelegate;
import jdk.jshell.spi.ExecutionControl;
import jdk.jshell.spi.ExecutionControlProvider;
import jdk.jshell.spi.ExecutionEnv;

public final class JShellSharedExecutionControlProvider
    implements ExecutionControlProvider
{
    private final ClassLoader parent;
    private ByteArrayOutputStream outBuffer;
    private ByteArrayOutputStream errBuffer;

    public JShellSharedExecutionControlProvider(ClassLoader parent)
    {
        this.parent = parent;
        this.outBuffer = null;
        this.errBuffer = null;
    }

    public void setOutputBuffers(ByteArrayOutputStream outBuffer, ByteArrayOutputStream errBuffer)
    {
        this.outBuffer = outBuffer;
        this.errBuffer = errBuffer;
    }

    @Override
    public String name()
    {
        return "local-shared"; //$NON-NLS-1$
    }

    @Override
    public ExecutionControl generate(ExecutionEnv env, Map<String, String> parameters)
    {
        return new CapturingExecutionControl(new SharedLoaderDelegate(parent), outBuffer, errBuffer);
    }

    private static final class SharedLoaderDelegate
        implements LoaderDelegate
    {
        private final SharedClassLoader loader;
        private final Map<String, Class<?>> klasses = new ConcurrentHashMap<>();

        private SharedLoaderDelegate(ClassLoader parent)
        {
            this.loader = new SharedClassLoader(parent);
        }

        @Override
        public void load(ExecutionControl.ClassBytecodes[] cbcs) throws ExecutionControl.ClassInstallException,
            ExecutionControl.NotImplementedException, ExecutionControl.EngineTerminationException
        {
            boolean[] loaded = new boolean[cbcs.length];
            try
            {
                for (ExecutionControl.ClassBytecodes cbc : cbcs)
                {
                    loader.declare(cbc.name(), cbc.bytecodes());
                }
                for (int i = 0; i < cbcs.length; ++i)
                {
                    ExecutionControl.ClassBytecodes cbc = cbcs[i];
                    Class<?> klass = loader.loadClass(cbc.name());
                    klasses.put(cbc.name(), klass);
                    loaded[i] = true;
                    // Force class preparation to surface linkage errors early.
                    klass.getDeclaredMethods();
                }
            }
            catch (Throwable ex)
            {
                throw new ExecutionControl.ClassInstallException("load: " + ex.getMessage(), loaded); //$NON-NLS-1$
            }
        }

        @Override
        public void classesRedefined(ExecutionControl.ClassBytecodes[] cbcs)
        {
            for (ExecutionControl.ClassBytecodes cbc : cbcs)
            {
                loader.declare(cbc.name(), cbc.bytecodes());
            }
        }

        @Override
        public void addToClasspath(String path)
            throws ExecutionControl.EngineTerminationException, ExecutionControl.InternalException
        {
            try
            {
                for (String entry : path.split(File.pathSeparator))
                {
                    loader.addURL(new File(entry).toURI().toURL());
                }
            }
            catch (Exception ex)
            {
                throw new ExecutionControl.InternalException(ex.toString());
            }
        }

        @Override
        public Class<?> findClass(String name) throws ClassNotFoundException
        {
            Class<?> klass = klasses.get(name);
            if (klass == null)
            {
                throw new ClassNotFoundException(name + " not found"); //$NON-NLS-1$
            }
            return klass;
        }
    }

    private static final class SharedClassLoader
        extends URLClassLoader
    {
        private final Map<String, byte[]> classBytes = new ConcurrentHashMap<>();
        private final ClassLoader osgiClassLoader;

        private SharedClassLoader(ClassLoader parent)
        {
            super(new URL[0], parent);
            this.osgiClassLoader = parent;
        }

        private void declare(String name, byte[] bytes)
        {
            classBytes.put(name, bytes);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException
        {
            synchronized (getClassLoadingLock(name))
            {
                Class<?> loadedClass = findLoadedClass(name);
                if (loadedClass != null)
                {
                    return loadedClass;
                }

                byte[] bytes = classBytes.get(name);
                if (bytes != null)
                {
                    loadedClass = defineClass(name, bytes, 0, bytes.length);
                    if (resolve)
                    {
                        resolveClass(loadedClass);
                    }
                    return loadedClass;
                }

                try
                {
                    loadedClass = osgiClassLoader.loadClass(name);
                    if (loadedClass != null)
                    {
                        if (resolve)
                        {
                            resolveClass(loadedClass);
                        }
                        return loadedClass;
                    }
                }
                catch (ClassNotFoundException e)
                {
                    //
                }

                return super.loadClass(name, resolve);
            }
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException
        {
            byte[] bytes = classBytes.get(name);
            if (bytes != null)
            {
                return defineClass(name, bytes, 0, bytes.length);
            }
            return super.findClass(name);
        }

        @Override
        public void addURL(URL url)
        {
            super.addURL(url);
        }
    }

    private static final class CapturingExecutionControl
        implements ExecutionControl
    {
        private final LoaderDelegate loaderDelegate;
        private final ByteArrayOutputStream outBuffer;
        private final ByteArrayOutputStream errBuffer;

        CapturingExecutionControl(LoaderDelegate loaderDelegate, ByteArrayOutputStream outBuffer,
            ByteArrayOutputStream errBuffer)
        {
            Preconditions.checkNotNull(loaderDelegate);
            Preconditions.checkNotNull(outBuffer);
            Preconditions.checkNotNull(errBuffer);

            this.loaderDelegate = loaderDelegate;
            this.outBuffer = outBuffer;
            this.errBuffer = errBuffer;
        }

        @Override
        public void close()
        {
            //
        }

        @Override
        public String invoke(String className, String methodName)
                throws RunException, EngineTerminationException, InternalException
        {
            PrintStream originalOut = System.out;
            PrintStream originalErr = System.err;
            try
            {
                System.setOut(new PrintStream(outBuffer, true));
                System.setErr(new PrintStream(errBuffer, true));
                Class<?> klass = loaderDelegate.findClass(className);
                Method method = klass.getDeclaredMethod(methodName);
                method.setAccessible(true);
                Object result = method.invoke(null);
                return result == null ? null : result.toString();
            }
            catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException e)
            {
                throw new InternalException(e.toString());
            }
            catch (InvocationTargetException e)
            {
                Throwable cause = e.getCause();
                if (cause instanceof RuntimeException)
                {
                    throw (RuntimeException)cause;
                }

                throw new RuntimeException(cause);
            }
            finally
            {
                System.setOut(originalOut);
                System.setErr(originalErr);
            }
        }

        @Override
        public Object extensionCommand(String command, Object arg)
                throws RunException, EngineTerminationException, InternalException
        {
            throw new NotImplementedException("extensionCommand"); //$NON-NLS-1$
        }

        @Override
        public void load(ClassBytecodes[] cbcs)
                throws ClassInstallException, NotImplementedException, EngineTerminationException
        {
            PrintStream originalOut = System.out;
            PrintStream originalErr = System.err;
            try
            {
                System.setOut(new PrintStream(outBuffer, true));
                System.setErr(new PrintStream(errBuffer, true));
                loaderDelegate.load(cbcs);
            }
            finally
            {
                System.setOut(originalOut);
                System.setErr(originalErr);
            }
        }

        @Override
        public void redefine(ClassBytecodes[] cbcs)
                throws ClassInstallException, NotImplementedException, EngineTerminationException
        {
            loaderDelegate.classesRedefined(cbcs);
        }

        @Override
        public void addToClasspath(String path)
                throws EngineTerminationException, InternalException
        {
            loaderDelegate.addToClasspath(path);
        }

        @Override
        public void stop() throws EngineTerminationException, InternalException
        {
            throw new NotImplementedException("stop"); //$NON-NLS-1$
        }

        @Override
        public String varValue(String className, String varName)
            throws EngineTerminationException, InternalException, RunException
        {
            throw new NotImplementedException("varValue"); //$NON-NLS-1$
        }
    }
}
