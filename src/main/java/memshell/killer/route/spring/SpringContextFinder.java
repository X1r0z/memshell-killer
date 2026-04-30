package memshell.killer.route.spring;

import memshell.killer.route.tomcat.TomcatContextFinder;
import memshell.killer.route.tomcat.TomcatSupport;
import memshell.killer.util.Reflects;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SpringContextFinder {
    public Set<Object> find() {
        Set<Object> contexts = new HashSet<>();
        Set<ClassLoader> loaders = new HashSet<>();
        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            ClassLoader loader = thread.getContextClassLoader();
            if (loader == null) {
                continue;
            }
            loaders.add(loader);
            addLiveBeansViewContexts(loader, contexts);
        }
        addTomcatContexts(contexts, loaders);
        addContextLoaderContexts(loaders, contexts);
        addRequestContext(contexts);
        return contexts;
    }

    public List<Object> handlerMappings() {
        Set<Object> mappings = new HashSet<>();
        for (Object context : find()) {
            Object mapping = SpringSupport.bean(context, "requestMappingHandlerMapping");
            if (mapping != null) {
                mappings.add(mapping);
            }
            Class<?> handlerMappingType = loadHandlerMappingType(context);
            if (handlerMappingType != null) {
                mappings.addAll(Reflects.asList(Reflects.invokeAnyQuiet(context, "getBeansOfType", handlerMappingType)));
            }
            for (String beanName : SpringSupport.COMMON_HANDLER_MAPPING_BEANS) {
                Object named = SpringSupport.bean(context, beanName);
                if (named != null) {
                    mappings.add(named);
                }
            }
        }
        return new ArrayList<>(mappings);
    }

    private void addRequestContext(Set<Object> contexts) {
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            Class<?> holder = loader.loadClass("org.springframework.web.context.request.RequestContextHolder");
            Object requestAttributes = Reflects.invokeQuiet(holder, "getRequestAttributes", null, null);
            Object request = requestAttributes == null ? null : Reflects.invokeQuiet(requestAttributes, "getRequest", null, null);
            Object context = request == null ? null : Reflects.invokeQuiet(request, "getAttribute", new Class[]{String.class}, new Object[]{"org.springframework.web.servlet.DispatcherServlet.CONTEXT"});
            if (context != null) {
                contexts.add(context);
            }
        } catch (Throwable ignored) {
        }
    }

    private void addLiveBeansViewContexts(ClassLoader loader, Set<Object> contexts) {
        try {
            Class<?> liveBeansView = loader.loadClass("org.springframework.context.support.LiveBeansView");
            Object view = liveBeansView.getDeclaredConstructor().newInstance();
            Object applicationContexts = Reflects.getQuiet(view, "applicationContexts");
            addSpringContexts(contexts, applicationContexts);
        } catch (Throwable ignored) {
            try {
                Class<?> liveBeansView = loader.loadClass("org.springframework.context.support.LiveBeansView");
                addSpringContexts(contexts, Reflects.getQuiet(liveBeansView, "applicationContexts"));
            } catch (Throwable ignoredAgain) {
            }
        }
    }

    private void addTomcatContexts(Set<Object> contexts, Set<ClassLoader> loaders) {
        for (Object tomcatContext : new TomcatContextFinder().find()) {
            ClassLoader loader = TomcatSupport.webAppClassLoader(tomcatContext);
            if (loader != null) {
                loaders.add(loader);
                addLiveBeansViewContexts(loader, contexts);
            }
            Object servletContext = Reflects.invokeAnyQuiet(tomcatContext, "getServletContext");
            addServletContextAttributes(contexts, servletContext);
            for (Object wrapper : Reflects.asList(Reflects.invokeAnyQuiet(tomcatContext, "findChildren"))) {
                Object servlet = Reflects.getQuiet(wrapper, "instance");
                if (servlet == null) {
                    servlet = Reflects.invokeAnyQuiet(wrapper, "getServlet");
                }
                addIfSpringContext(contexts, Reflects.invokeAnyQuiet(servlet, "getWebApplicationContext"));
                addIfSpringContext(contexts, Reflects.getQuiet(servlet, "webApplicationContext"));
            }
        }
    }

    private void addServletContextAttributes(Set<Object> contexts, Object servletContext) {
        for (Object rawName : Reflects.asList(Reflects.invokeAnyQuiet(servletContext, "getAttributeNames"))) {
            String name = String.valueOf(rawName);
            if (name.contains("WebApplicationContext") || name.contains("FrameworkServlet.CONTEXT")) {
                addIfSpringContext(contexts, Reflects.invokeAnyQuiet(servletContext, "getAttribute", name));
            }
        }
    }

    private void addContextLoaderContexts(Set<ClassLoader> loaders, Set<Object> contexts) {
        for (ClassLoader loader : loaders) {
            try {
                Class<?> contextLoader = loader.loadClass("org.springframework.web.context.ContextLoader");
                addIfSpringContext(contexts, Reflects.getQuiet(contextLoader, "currentContext"));
                addSpringContexts(contexts, Reflects.getQuiet(contextLoader, "currentContextPerThread"));
            } catch (Throwable ignored) {
            }
            try {
                Class<?> listener = loader.loadClass("org.springframework.web.context.ContextLoaderListener");
                addIfSpringContext(contexts, Reflects.getQuiet(listener, "context"));
            } catch (Throwable ignored) {
            }
        }
    }

    private void addSpringContexts(Set<Object> contexts, Object values) {
        for (Object value : Reflects.asList(values)) {
            addIfSpringContext(contexts, value);
        }
    }

    private void addIfSpringContext(Set<Object> contexts, Object value) {
        if (value != null && value.getClass().getName().contains("ApplicationContext")) {
            contexts.add(value);
        }
    }

    private Class<?> loadHandlerMappingType(Object context) {
        ClassLoader loader = context == null ? null : context.getClass().getClassLoader();
        try {
            return Class.forName("org.springframework.web.servlet.HandlerMapping", false, loader);
        } catch (Throwable ignored) {
            try {
                return Class.forName("org.springframework.web.servlet.HandlerMapping");
            } catch (Throwable ignoredAgain) {
                return null;
            }
        }
    }
}
