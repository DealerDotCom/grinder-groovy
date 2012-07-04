package net.grinder.scriptengine.groovy;

import groovy.lang.GroovyObject;
import net.grinder.engine.common.EngineException;
import net.grinder.engine.common.ScriptLocation;
import net.grinder.scriptengine.ScriptEngineService;
import net.grinder.scriptengine.ScriptExecutionException;
import net.grinder.testutility.AbstractJUnit4FileTestCase;
import net.grinder.util.Directory;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;

import static net.grinder.testutility.AssertUtilities.assertContains;
import static net.grinder.testutility.AssertUtilities.assertStartsWith;
import static net.grinder.testutility.FileUtilities.createFile;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Ryan Gardner
 */
public class TestGroovyScriptEngine extends AbstractJUnit4FileTestCase {


    @Test
    public void testEngineCreationWithBasicClosure() throws IOException, EngineException {
        final ScriptLocation script =
                new ScriptLocation(new Directory(getDirectory()), new File("test.groovy"));

        createFile(script.getFile(),
                "class foo {" +
                        "def testRunner = { println \"called runner\"}" +
                        "}");

        new GroovyScriptEngineService().createScriptEngine(script);
    }

    @Test
    public void testEngineCreationWithNoClassWrappingClosure() throws IOException, EngineException {
        final ScriptLocation script =
                new ScriptLocation(new Directory(getDirectory()), new File("testNoWrappingClass.groovy"));

        createFile(script.getFile(), "def testRunner = { println \"called runner\"}");
        try {
            new GroovyScriptEngineService().createScriptEngine(script);
            fail("Expected Exception because there is no class wrapping the closure");
        } catch (EngineException e) {
            assertContains(e.getMessage(), "Unable to locate the closure named");
        }
    }

    @Test
    public void testEngineCreationWithNoClosure() throws IOException, EngineException {
        final ScriptLocation script =
                new ScriptLocation(new Directory(getDirectory()), new File("testNoClosure.groovy"));

        createFile(script.getFile(), "class foo {  }");
        try {
            new GroovyScriptEngineService().createScriptEngine(script);
            fail("Expected ScriptExecutionException because there is no class wrapping the closure");
        } catch (EngineException e) {
            assertContains(e.getMessage(), "Unable to locate the closure named");
        }
    }

    @Test
    public void testGroovyClosureExceptionsCaught() throws Exception {

        final ScriptLocation script =
                new ScriptLocation(new Directory(getDirectory()), new File("exception.groovy"));

        createFile(script.getFile(),
                "class foo { def testRunner = { throw new UnsupportedOperationException('exception from thread') } }");

        final ScriptEngineService.ScriptEngine scriptEngine =
                new GroovyScriptEngineService().createScriptEngine(script);

        try {
            scriptEngine.createWorkerRunnable().run();
            fail("Expected ScriptExecutionException");
        } catch (ScriptExecutionException e) {
            assertTrue(e.getCause() instanceof UnsupportedOperationException);
        }
    }

    @Test
    public void testGroovyDescription() throws Exception {
        final ScriptLocation script =
                new ScriptLocation(new Directory(getDirectory()), new File("bogus.groovy"));

        createFile(script.getFile(),
                "class foo { def testRunner = { throw new UnsupportedOperationException('exception from thread') } }");


        String description = new GroovyScriptEngineService().createScriptEngine(script).getDescription();
        assertStartsWith(description, "GroovyScriptEngine running with groovy version: ");
    }

    @Test
    public void testGroovyRunnerFromObject() throws Exception {
        GroovyObject obj = Mockito.mock(GroovyObject.class);


        final ScriptLocation script =
                new ScriptLocation(new Directory(getDirectory()), new File("notused.groovy"));

        createFile(script.getFile(),
                "class groovyClass { def testRunner = { } }");

        ScriptEngineService.WorkerRunnable runnable = new GroovyScriptEngine(script).createWorkerRunnable(obj);
        assertNotNull(runnable);
    }

    @Test(expected = GroovyScriptEngine.GroovyScriptExecutionException.class)
    public void exceptionThrownWhenCreatedWithNonCallableObject() throws Exception {
        String foo = "this string is obviously not a callable";
        final ScriptLocation script =
                new ScriptLocation(new Directory(getDirectory()), new File("notused.groovy"));

        createFile(script.getFile(),
                "class groovyClass { def testRunner = { } }");

        ScriptEngineService.WorkerRunnable runnable = new GroovyScriptEngine(script).createWorkerRunnable(foo);
        assertNotNull(runnable);

    }

    @Test(expected = EngineException.class)
    public void testIoException() throws Exception {
        final ScriptLocation script =
                new ScriptLocation(new Directory(getDirectory()), new File("bogus.groovy"));

        new GroovyScriptEngineService().createScriptEngine(script);
    }

}
