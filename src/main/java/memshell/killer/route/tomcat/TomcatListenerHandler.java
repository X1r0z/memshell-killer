package memshell.killer.route.tomcat;

import memshell.killer.route.RouteType;
import memshell.killer.core.RemoveResult;
import memshell.killer.core.DumpResult;
import memshell.killer.route.RouteHandler;
import memshell.killer.util.Reflects;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

public class TomcatListenerHandler implements RouteHandler {
    @Override
    public String type() {
        return RouteType.LISTENER;
    }

    @Override
    public List<DumpResult> dump() {
        List<DumpResult> entries = new ArrayList<>();
        for (Object context : contexts()) {
            addListeners(context, entries, applicationListeners(context));
            addListeners(context, entries, lifecycleListeners(context));
        }
        return entries;
    }

    @Override
    public RemoveResult remove(String className) throws Exception {
        RemoveResult result = new RemoveResult();
        result.type = type();
        result.className = className;
        for (Object context : contexts()) {
            Set<Object> removedListeners = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
            result.removed += removeApplicationListeners(context, className, result, removedListeners);
            result.removed += removeLifecycleListeners(context, className, result, removedListeners);
        }
        return result;
    }

    private void addListeners(Object context, List<DumpResult> entries, Object listeners) {
        for (Object listener : Reflects.asList(listeners)) {
            if (listener != null) {
                entries.add(TomcatSupport.entry(type(), context, listener.getClass().getName(), Collections.singletonList("/*"), listener.getClass()));
            }
        }
    }

    protected Iterable<Object> contexts() {
        return new TomcatContextFinder().find();
    }

    private int removeApplicationListeners(Object context, String className, RemoveResult result, Set<Object> removedListeners) {
        int removed = 0;
        removed += removeFromCollectionField(context, "applicationEventListenersList", className, removedListeners);
        removed += removeFromArrayField(context, "applicationEventListenersObjects", className, removedListeners);
        Object listeners = Reflects.invokeQuiet(context, "getApplicationEventListeners", null, null);
        if (listeners instanceof List) {
            int removedHere = removeFromList((List<?>) listeners, className, removedListeners);
            removed += removedHere;
        } else if (listeners != null && listeners.getClass().isArray()) {
            removed += removeReturnedArray(context, listeners, className, removedListeners);
        }
        if (removed > 0) {
            result.details.add(TomcatSupport.contextName(context) + " listener " + className);
        }
        return removed;
    }

    private int removeLifecycleListeners(Object context, String className, RemoveResult result, Set<Object> removedListeners) {
        int removed = 0;
        for (Object listener : new ArrayList<>(Reflects.asList(lifecycleListeners(context)))) {
            if (listener == null || !listener.getClass().getName().equals(className)) {
                continue;
            }
            int before = Reflects.asList(lifecycleListeners(context)).size();
            Reflects.invokeAnyQuiet(context, "removeLifecycleListener", listener);
            int after = Reflects.asList(lifecycleListeners(context)).size();
            if (after < before) {
                removed += removedListeners.add(listener) ? 1 : 0;
            }
        }
        removed += removeFromCollectionField(context, "lifecycleListeners", className, removedListeners);
        removed += removeFromArrayField(context, "lifecycleListeners", className, removedListeners);
        if (removed > 0) {
            result.details.add(TomcatSupport.contextName(context) + " lifecycleListener " + className);
        }
        return removed;
    }

    private Object applicationListeners(Object context) {
        Object listeners = Reflects.invokeQuiet(context, "getApplicationEventListeners", null, null);
        if (listeners != null) {
            return listeners;
        }
        Object list = Reflects.getQuiet(context, "applicationEventListenersList");
        return list == null ? Reflects.getQuiet(context, "applicationEventListenersObjects") : list;
    }

    private Object lifecycleListeners(Object context) {
        Object listeners = Reflects.invokeQuiet(context, "findLifecycleListeners", null, null);
        return listeners == null ? Reflects.getQuiet(context, "lifecycleListeners") : listeners;
    }

    private int removeFromList(List<?> list, String className, Set<Object> removedListeners) {
        int removed = 0;
        for (int i = list.size() - 1; i >= 0; i--) {
            Object item = list.get(i);
            if (item != null && item.getClass().getName().equals(className)) {
                list.remove(i);
                removed += removedListeners.add(item) ? 1 : 0;
            }
        }
        return removed;
    }

    private int removeFromCollectionField(Object context, String fieldName, String className, Set<Object> removedListeners) {
        Object value = Reflects.getQuiet(context, fieldName);
        if (!(value instanceof List)) {
            return 0;
        }
        return removeFromList((List<?>) value, className, removedListeners);
    }

    private int removeFromArrayField(Object context, String fieldName, String className, Set<Object> removedListeners) {
        Object listeners = Reflects.getQuiet(context, fieldName);
        if (listeners == null || !listeners.getClass().isArray()) {
            return 0;
        }
        List<Object> kept = new ArrayList<>();
        List<Object> removed = new ArrayList<>();
        for (Object listener : Reflects.asList(listeners)) {
            if (listener != null && listener.getClass().getName().equals(className)) {
                removed.add(listener);
            } else {
                kept.add(listener);
            }
        }
        if (removed.isEmpty()) {
            return 0;
        }
        try {
            Reflects.set(context, fieldName, arrayOf(listeners.getClass().getComponentType(), kept));
            return rememberRemoved(removedListeners, removed);
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private int removeReturnedArray(Object context, Object listeners, String className, Set<Object> removedListeners) {
        List<Object> kept = new ArrayList<>();
        List<Object> removed = new ArrayList<>();
        for (Object listener : Reflects.asList(listeners)) {
            if (listener != null && listener.getClass().getName().equals(className)) {
                removed.add(listener);
            } else {
                kept.add(listener);
            }
        }
        if (removed.isEmpty()) {
            return 0;
        }
        Reflects.invokeAnyQuiet(context, "setApplicationEventListeners", arrayOf(listeners.getClass().getComponentType(), kept));
        Object after = Reflects.invokeQuiet(context, "getApplicationEventListeners", null, null);
        return Reflects.asList(after).size() == kept.size() ? rememberRemoved(removedListeners, removed) : 0;
    }

    private Object arrayOf(Class<?> component, List<Object> values) {
        Object array = Array.newInstance(component, values.size());
        for (int i = 0; i < values.size(); i++) {
            Array.set(array, i, values.get(i));
        }
        return array;
    }

    private int rememberRemoved(Set<Object> removedListeners, List<Object> listeners) {
        int added = 0;
        for (Object listener : listeners) {
            added += removedListeners.add(listener) ? 1 : 0;
        }
        return added;
    }
}
