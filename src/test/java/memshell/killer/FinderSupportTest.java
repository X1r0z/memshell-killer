package memshell.killer;

import memshell.killer.route.tomcat.TomcatContextFinder;
import memshell.killer.route.tomcat.TomcatSupport;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class FinderSupportTest {
    @Test
    public void tomcatContextFinderUsesStandardContextStaticInstances() {
        Object instanceContext = new Object();
        Object lastContext = new Object();
        org.apache.catalina.core.StandardContext.instances.clear();
        org.apache.catalina.core.StandardContext.instances.put("app", instanceContext);
        org.apache.catalina.core.StandardContext.lastContext = lastContext;

        Set<Object> contexts = new TomcatContextFinder().find();

        assertTrue(contexts.contains(instanceContext));
        assertTrue(contexts.contains(lastContext));
    }

    @Test
    public void webAppClassLoaderPrefersNestedLoader() {
        ClassLoader nested = new MarkerClassLoader();
        WebContext context = new WebContext(nested);

        assertSame(nested, TomcatSupport.webAppClassLoader(context));
    }

    static class WebContext {
        private final Loader loader;

        WebContext(ClassLoader classLoader) {
            this.loader = new Loader(classLoader);
        }

        Loader getLoader() {
            return loader;
        }
    }

    static class Loader {
        private final ClassLoader classLoader;

        Loader(ClassLoader classLoader) {
            this.classLoader = classLoader;
        }

        ClassLoader getClassLoader() {
            return classLoader;
        }
    }

    static class MarkerClassLoader extends ClassLoader {
    }
}
