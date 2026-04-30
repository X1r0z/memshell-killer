package memshell.killer.route.tomcat;

import memshell.killer.util.Reflects;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TomcatContextFinder {
    public Set<Object> find() {
        Set<Object> contexts = new HashSet<>();
        Set<ClassLoader> loaders = new HashSet<>();
        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            if (thread.getContextClassLoader() != null) {
                loaders.add(thread.getContextClassLoader());
            }
            String name = thread.getName();
            if (name.contains("ContainerBackgroundProcessor")) {
                addBackgroundProcessorContexts(thread, contexts);
            } else if (name.contains("Poller") && !name.contains("ajp")) {
                addPollerContexts(thread, contexts);
            }
            addClassLoaderContext(thread, contexts);
        }
        addStandardContextStaticInstances(loaders, contexts);
        return contexts;
    }

    private void addStandardContextStaticInstances(Set<ClassLoader> loaders, Set<Object> contexts) {
        for (ClassLoader loader : loaders) {
            try {
                Class<?> standardContext = Class.forName("org.apache.catalina.core.StandardContext", false, loader);
                Object instances = Reflects.getQuiet(standardContext, "instances");
                for (Object context : Reflects.asList(instances)) {
                    if (context != null) {
                        contexts.add(context);
                    }
                }
                Object lastContext = Reflects.getQuiet(standardContext, "lastContext");
                if (lastContext != null) {
                    contexts.add(lastContext);
                }
            } catch (Throwable ignored) {
            }
        }
    }

    private void addBackgroundProcessorContexts(Thread thread, Set<Object> contexts) {
        try {
            Object target = Reflects.get(thread, "target");
            Object parent = Reflects.get(target, "this$0");
            Map<?, ?> childrenMap = (Map<?, ?>) Reflects.get(parent, "children");
            addChildrenContexts(childrenMap, contexts);
        } catch (Throwable ignored) {
        }
    }

    private void addPollerContexts(Thread thread, Set<Object> contexts) {
        try {
            Object proto = Reflects.get(Reflects.get(Reflects.get(Reflects.get(thread, "target"), "this$0"), "handler"), "proto");
            Object engine = Reflects.get(Reflects.get(Reflects.get(Reflects.get(proto, "adapter"), "connector"), "service"), "engine");
            Map<?, ?> childrenMap = (Map<?, ?>) Reflects.get(engine, "children");
            addChildrenContexts(childrenMap, contexts);
        } catch (Throwable ignored) {
        }
    }

    private void addChildrenContexts(Map<?, ?> childrenMap, Set<Object> contexts) {
        if (childrenMap == null) {
            return;
        }
        for (Object value : childrenMap.values()) {
            Object children = Reflects.getQuiet(value, "children");
            if (children instanceof Map) {
                contexts.addAll(((Map<?, ?>) children).values());
            }
        }
    }

    private void addClassLoaderContext(Thread thread, Set<Object> contexts) {
        try {
            ClassLoader loader = thread.getContextClassLoader();
            if (loader == null || !loader.getClass().getSimpleName().matches(".+WebappClassLoader.*")) {
                return;
            }
            Object resources = Reflects.getQuiet(loader, "resources");
            if (resources != null && resources.getClass().getName().endsWith("Root")) {
                Object context = Reflects.getQuiet(resources, "context");
                if (context != null) {
                    contexts.add(context);
                }
            }
        } catch (Throwable ignored) {
        }
    }
}
