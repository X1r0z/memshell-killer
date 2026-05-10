package memshell.killer;

import memshell.killer.core.RemoveResult;
import memshell.killer.core.DumpResult;
import memshell.killer.route.tomcat.TomcatListenerHandler;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class TomcatListenerHandlerTest {
    @Test
    public void bareListenerArrayReturnDoesNotReportRemoval() throws Exception {
        BareArrayContext context = new BareArrayContext();

        RemoveResult result = handler(context).remove(BadListener.class.getName());

        assertEquals(0, result.removed);
        assertEquals(2, context.listeners.length);
    }

    @Test
    public void returnedListenerArrayIsRemovedThroughSetter() throws Exception {
        SetterArrayContext context = new SetterArrayContext();

        RemoveResult result = handler(context).remove(BadListener.class.getName());

        assertEquals(1, result.removed);
        assertEquals(1, context.listeners.length);
        assertEquals(SafeListener.class, context.listeners[0].getClass());
    }

    @Test
    public void listenerArrayFieldRemovalIsCountedAfterWriteBack() throws Exception {
        ArrayFieldContext context = new ArrayFieldContext();

        RemoveResult result = handler(context).remove(BadListener.class.getName());

        assertEquals(1, result.removed);
        assertEquals(1, context.applicationEventListenersObjects.length);
        assertEquals(SafeListener.class, context.applicationEventListenersObjects[0].getClass());
    }

    @Test
    public void duplicateApplicationListenerContainersCountIdentityOnce() throws Exception {
        DuplicateApplicationContext context = new DuplicateApplicationContext();

        RemoveResult result = handler(context).remove(BadListener.class.getName());

        assertEquals(1, result.removed);
        assertEquals(1, context.applicationEventListenersList.size());
        assertEquals(SafeListener.class, context.applicationEventListenersList.get(0).getClass());
        assertEquals(1, context.applicationEventListenersObjects.length);
        assertEquals(SafeListener.class, context.applicationEventListenersObjects[0].getClass());
    }

    @Test
    public void dumpAndRemoveLifecycleListeners() throws Exception {
        LifecycleMethodContext context = new LifecycleMethodContext();

        List<DumpResult> routes = handler(context).dump();
        RemoveResult result = handler(context).remove(BadListener.class.getName());

        assertFalse(routes.isEmpty());
        assertTrue(containsClass(routes, BadListener.class.getName()));
        assertEquals(1, result.removed);
        assertEquals(1, context.lifecycleListeners.length);
        assertEquals(SafeListener.class, context.lifecycleListeners[0].getClass());
    }

    @Test
    public void duplicateLifecycleApiAndFieldRemovalCountsIdentityOnce() throws Exception {
        DuplicateLifecycleContext context = new DuplicateLifecycleContext();

        RemoveResult result = handler(context).remove(BadListener.class.getName());

        assertEquals(1, result.removed);
        assertEquals(1, context.liveLifecycleListeners.length);
        assertEquals(1, context.lifecycleListeners.length);
        assertEquals(SafeListener.class, context.liveLifecycleListeners[0].getClass());
        assertEquals(SafeListener.class, context.lifecycleListeners[0].getClass());
    }

    private boolean containsClass(List<DumpResult> routes, String className) {
        for (DumpResult route : routes) {
            if (className.equals(route.className)) {
                return true;
            }
        }
        return false;
    }

    private TestHandler handler(Object context) {
        return new TestHandler(Arrays.asList(context));
    }

    static class TestHandler extends TomcatListenerHandler {
        private final Iterable<Object> contexts;

        TestHandler(Iterable<Object> contexts) {
            this.contexts = contexts;
        }

        @Override
        protected Iterable<Object> contexts() {
            return contexts;
        }
    }

    static class BareArrayContext {
        Object[] listeners = {new BadListener(), new SafeListener()};

        Object getApplicationEventListeners() {
            return listeners;
        }
    }

    static class SetterArrayContext {
        Object[] listeners = {new BadListener(), new SafeListener()};

        Object getApplicationEventListeners() {
            return listeners;
        }

        void setApplicationEventListeners(Object[] listeners) {
            this.listeners = listeners;
        }
    }

    static class ArrayFieldContext {
        Object[] applicationEventListenersObjects = {new BadListener(), new SafeListener()};

        Object getApplicationEventListeners() {
            return null;
        }
    }

    static class DuplicateApplicationContext {
        BadListener bad = new BadListener();
        List<Object> applicationEventListenersList = new ArrayList<Object>(Arrays.asList(bad, new SafeListener()));
        Object[] applicationEventListenersObjects = {bad, new SafeListener()};

        Object getApplicationEventListeners() {
            return applicationEventListenersList;
        }
    }

    static class LifecycleMethodContext {
        Object[] lifecycleListeners = {new BadListener(), new SafeListener()};

        Object getApplicationEventListeners() {
            return null;
        }

        Object[] findLifecycleListeners() {
            return lifecycleListeners;
        }

        void removeLifecycleListener(Object listener) {
            lifecycleListeners = Arrays.stream(lifecycleListeners)
                    .filter(item -> item != listener)
                    .toArray(Object[]::new);
        }
    }

    static class DuplicateLifecycleContext {
        BadListener bad = new BadListener();
        Object[] liveLifecycleListeners = {bad, new SafeListener()};
        Object[] lifecycleListeners = {bad, new SafeListener()};

        Object getApplicationEventListeners() {
            return null;
        }

        Object[] findLifecycleListeners() {
            return liveLifecycleListeners;
        }

        void removeLifecycleListener(Object listener) {
            liveLifecycleListeners = Arrays.stream(liveLifecycleListeners)
                    .filter(item -> item != listener)
                    .toArray(Object[]::new);
        }
    }

    static class BadListener {
    }

    static class SafeListener {
    }
}
