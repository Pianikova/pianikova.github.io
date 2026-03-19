/**
 *
 */
package com.e1c.edt.ai.tools;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

import jdk.jshell.execution.LoaderDelegate;
import jdk.jshell.execution.LocalExecutionControl;
import jdk.jshell.spi.ExecutionControl;
import jdk.jshell.spi.ExecutionControlProvider;
import jdk.jshell.spi.ExecutionEnv;

public final class SharedExecutionControlProvider
    implements ExecutionControlProvider
{
    private final ClassLoader parent;

    public SharedExecutionControlProvider(ClassLoader parent)
    {
        this.parent = parent;
    }

    @Override
    public String name()
    {
        return "local-shared"; //$NON-NLS-1$
    }

    @Override
    public ExecutionControl generate(ExecutionEnv env, Map<String, String> parameters)
    {
        return new LocalExecutionControl(new SharedLoaderDelegate(parent));
    }

    private static final class SharedLoaderDelegate
        implements LoaderDelegate
    {
        private final SharedClassLoader loader;
        private final Map<String, Class<?>> klasses = new HashMap<>();

        private SharedLoaderDelegate(ClassLoader parent)
        {
            this.loader = new SharedClassLoader(parent);
            Thread.currentThread().setContextClassLoader(loader);
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
        private final Map<String, byte[]> classBytes = new HashMap<>();

        private SharedClassLoader(ClassLoader parent)
        {
            super(new URL[0], parent);
        }

        private void declare(String name, byte[] bytes)
        {
            classBytes.put(name, bytes);
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
}
