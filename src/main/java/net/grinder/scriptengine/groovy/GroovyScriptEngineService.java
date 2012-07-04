package net.grinder.scriptengine.groovy;

import net.grinder.engine.common.EngineException;
import net.grinder.engine.common.ScriptLocation;
import net.grinder.scriptengine.Instrumenter;
import net.grinder.scriptengine.ScriptEngineService;
import net.grinder.util.FileExtensionMatcher;

import java.util.List;

import static java.util.Collections.emptyList;

/**
 * Groovy implementation of the {@link ScriptEngineService}.
 *
 * @author Ryan Gardner
 */
public class GroovyScriptEngineService implements ScriptEngineService {

    private final FileExtensionMatcher m_cljFileMatcher =
            new FileExtensionMatcher(".groovy");

    /**
     * {@inheritDoc}
     */
    @Override
    public ScriptEngine createScriptEngine(ScriptLocation script)
            throws EngineException {
        if (m_cljFileMatcher.accept(script.getFile())) {
            try {
                return new GroovyScriptEngine(script);
            }
            catch (LinkageError e) {
                throw new EngineException("Groovy is not on the classpath", e);
            }
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<? extends Instrumenter> createInstrumenters()
            throws EngineException {
        return emptyList();
    }
}
