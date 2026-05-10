package memshell.killer.route.tomcat;

import memshell.killer.core.DumpResult;
import memshell.killer.inspect.ClassIntrospector;
import memshell.killer.util.Reflects;

import java.lang.reflect.Array;
import java.util.*;

public final class TomcatSupport {
    private TomcatSupport() {
    }

    public static String contextName(Object context) {
        Object servletContext = Reflects.invokeQuiet(context, "getServletContext", null, null);
        Object path = servletContext == null ? null : Reflects.invokeQuiet(servletContext, "getContextPath", null, null);
        String suffix = path == null ? "" : "(" + (String.valueOf(path).isEmpty() ? "/" : path) + ")";
        return context.getClass().getName() + suffix;
    }

    public static DumpResult entry(String type, Object context, String name, List<String> routes, Class<?> clazz) {
        DumpResult entry = new DumpResult();
        entry.type = type;
        entry.context = contextName(context);
        entry.name = name;
        entry.routes.addAll(routes == null || routes.isEmpty() ? Collections.singletonList("/*") : routes);
        entry.className = clazz == null ? null : clazz.getName();
        if (clazz != null) {
            entry.classInfo = ClassIntrospector.inspect(clazz);
        }
        return entry;
    }

    public static Class<?> classFromObjectOrName(Object object, String className, ClassLoader loader) {
        if (object != null) {
            return object.getClass();
        }
        return classForName(className, loader);
    }

    public static Class<?> classForName(String className, ClassLoader preferredLoader) {
        if (className == null || className.trim().isEmpty()) {
            return null;
        }
        Set<ClassLoader> loaders = new LinkedHashSet<>();
        if (preferredLoader != null) {
            loaders.add(preferredLoader);
        }
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        if (tccl != null) {
            loaders.add(tccl);
        }
        loaders.add(TomcatSupport.class.getClassLoader());
        for (ClassLoader loader : loaders) {
            try {
                return Class.forName(className, false, loader);
            } catch (Throwable ignored) {
            }
        }
        try {
            return Class.forName(className);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static ClassLoader webAppClassLoader(Object context) {
        if (context == null) {
            return null;
        }
        Object loader = Reflects.invokeAnyQuiet(context, "getLoader");
        Object nested = Reflects.invokeAnyQuiet(loader, "getClassLoader");
        if (nested instanceof ClassLoader) {
            return (ClassLoader) nested;
        }
        Object direct = Reflects.invokeAnyQuiet(context, "getClassLoader");
        if (direct instanceof ClassLoader) {
            return (ClassLoader) direct;
        }
        return context.getClass().getClassLoader();
    }

    public static List<String> stringList(Object value) {
        List<String> strings = new ArrayList<>();
        if (value == null) {
            return strings;
        }
        if (value instanceof Iterable) {
            for (Object item : (Iterable<?>) value) {
                strings.add(String.valueOf(item));
            }
            return strings;
        }
        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                strings.add(String.valueOf(Array.get(value, i)));
            }
            return strings;
        }
        strings.add(String.valueOf(value));
        return strings;
    }

    public static List<String> filterMapRoutes(Object filterMap) {
        Object patterns = Reflects.invokeQuiet(filterMap, "getURLPatterns", null, null);
        if (patterns == null) {
            patterns = Reflects.invokeQuiet(filterMap, "getURLPattern", null, null);
        }
        if (patterns == null) {
            patterns = Reflects.getQuiet(filterMap, "urlPatterns");
        }
        return stringList(patterns);
    }
}
