package net.grinder.scriptengine.groovy;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import groovy.lang.GroovySystem;
import groovy.lang.MissingPropertyException;
import net.grinder.engine.common.EngineException;
import net.grinder.engine.common.ScriptLocation;
import net.grinder.scriptengine.ScriptEngineService;
import net.grinder.scriptengine.ScriptEngineService.ScriptEngine;
import net.grinder.scriptengine.ScriptExecutionException;

import java.io.IOException;
import java.util.concurrent.Callable;

/**
 * Groovy implementation of {@link ScriptEngine}.
 *
 * @author Ryan Gardner
 */
public class GroovyScriptEngine implements ScriptEngine {
    private static final String TEST_RUNNER_CLOSURE_NAME = "testRunner";

    private final GroovyObject m_groovyObject;

    /**
     * Construct a GroovyScriptEngine that will use the supplied ScriptLocation.
     *
     * @param script location of the .groovy script file
     * @throws EngineException if there is an exception loading, parsing,
     *                         or constructing the test from the file.
     */
    public GroovyScriptEngine(ScriptLocation script) throws EngineException {
        // Get groovy to compile the script and access the callable closure
        final ClassLoader parent = getClass().getClassLoader();
        final GroovyClassLoader loader = new GroovyClassLoader(parent);

        try {
            final Class testRunnerClass = loader.parseClass(script.getFile());

            // fail fast if there is no appropriate closure to call
            final GroovyObject groovyObject =
                    (GroovyObject) testRunnerClass.newInstance();
            // test that the method exists - fail fast otherwise
            final Callable<?> closure =
                    (Callable<?>) groovyObject
                                     .getProperty(TEST_RUNNER_CLOSURE_NAME);
            assert closure != null;

            m_groovyObject = groovyObject;
        }
        catch (IOException io) {
            throw new EngineException("Unable to parse groovy script at: " +
                                       script.getFile().getAbsolutePath(), io);
        }
        catch (MissingPropertyException mpe) {
            throw new EngineException(
                String.format("Unable to locate the closure named " +
                    "\"%s\" in the script at: %s. Make sure that you " +
                    "have your groovy script wrapped in a class, and " +
                    "inside that class that there is a closure named: \"%s\"",
                        TEST_RUNNER_CLOSURE_NAME,
                        script.getFile().getAbsolutePath(),
                        TEST_RUNNER_CLOSURE_NAME),
                mpe);
        }
        catch (InstantiationException e) {
            throw new EngineException(
                 String.format("Unable to instantiate class from " +
                     "script at: %s",
                         script.getFile().getAbsolutePath()), e);
        }
        catch (IllegalAccessException e) {
            throw new EngineException(
                 String.format("Unable to acces class from " +
                     "script at: %s",
                     script.getFile().getAbsolutePath()), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ScriptEngineService.WorkerRunnable
            createWorkerRunnable() throws EngineException {
        return new GroovyWorkerRunnable(m_groovyObject);
    }


    /**
     * Wrapper for groovy's testRunner closure.
     */
    private final class GroovyWorkerRunnable
            implements ScriptEngineService.WorkerRunnable {

        private final GroovyObject m_groovyObject;

        private GroovyWorkerRunnable(GroovyObject groovyObject) {
            this.m_groovyObject = groovyObject;
        }

        @Override
        public void run() throws ScriptExecutionException {
            try {
                m_groovyObject.
                    invokeMethod(TEST_RUNNER_CLOSURE_NAME, new Object[]{});
            }
            catch (Exception e) {
                throw new GroovyScriptExecutionException(
                    "Exception raised by worker thread", e);
            }
        }

        @Override
        public void shutdown() throws ScriptExecutionException {
            // nothing to do here
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ScriptEngineService.WorkerRunnable
        createWorkerRunnable(Object testRunner) throws EngineException {
        if (testRunner instanceof GroovyObject) {
            return new GroovyWorkerRunnable((GroovyObject) testRunner);
        }

        throw new GroovyScriptExecutionException(
            "supplied testRunner object is not a groovy object");
    }

    /**
     * Shut down the engine.
     *
     * @throws net.grinder.engine.common.EngineException
     *          If the engine could not be shut down.
     */
    @Override
    public void shutdown() throws EngineException {
        // nothing is necessary
    }

    /**
     * Returns a description of the script engine for the log.
     *
     * @return The description.
     */
    @Override
    public String getDescription() {
        return String.format("GroovyScriptEngine running " +
                "with groovy version: %s", GroovySystem.getVersion());
    }

    /**
     * Exception thrown when an error occurs executing
     * a GroovyScript.
     */
    protected static final class GroovyScriptExecutionException
            extends ScriptExecutionException {

        /**
         * Construct an exception with the supplied message.
         * @param message the message for the exception
         */
        public GroovyScriptExecutionException(String message) {
            super(message);
        }

        /**
         * Construct an exception with the supplied message
         * and throwable.
         * @param s the message for the exception
         * @param t another throwable that this exception wraps
         */
        public GroovyScriptExecutionException(String s, Throwable t) {
            super(s, t);
        }
    }
}
