package memshell.killer;

import memshell.killer.route.spring.SpringContextFinder;
import memshell.killer.route.tomcat.TomcatContextFinder;
import memshell.killer.route.tomcat.TomcatSupport;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
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

    @Test
    public void springContextFinderProbesEachClassLoaderOnce() throws Exception {
        ClassLoader loader = new MarkerClassLoader();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        Thread first = threadWithContextLoader(loader, ready, release);
        Thread second = threadWithContextLoader(loader, ready, release);
        first.start();
        second.start();
        ready.await();

        CountingSpringContextFinder finder = new CountingSpringContextFinder();
        try {
            finder.find();
        } finally {
            release.countDown();
            first.join();
            second.join();
        }

        assertEquals(1, finder.counts.get(loader).intValue());
    }

    private Thread threadWithContextLoader(final ClassLoader loader, final CountDownLatch ready, final CountDownLatch release) {
        return new Thread(new Runnable() {
            @Override
            public void run() {
                Thread.currentThread().setContextClassLoader(loader);
                ready.countDown();
                try {
                    release.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
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

    static class CountingSpringContextFinder extends SpringContextFinder {
        final Map<ClassLoader, Integer> counts = new HashMap<ClassLoader, Integer>();

        @Override
        protected void addLiveBeansViewContexts(ClassLoader loader, Set<Object> contexts) {
            Integer count = counts.get(loader);
            counts.put(loader, count == null ? 1 : count + 1);
        }
    }
}
