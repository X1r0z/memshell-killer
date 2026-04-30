package memshell.killer.route.spring;

import memshell.killer.route.RouteEntry;
import memshell.killer.util.ClassIntrospector;
import memshell.killer.util.Reflects;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class SpringSupport {
    public static final List<String> COMMON_HANDLER_MAPPING_BEANS = Arrays.asList(
            "requestMappingHandlerMapping",
            "beanNameHandlerMapping",
            "viewControllerHandlerMapping",
            "resourceHandlerMapping"
    );

    private SpringSupport() {
    }

    public static Object bean(Object context, String name) {
        return Reflects.invokeQuiet(context, "getBean", new Class[]{String.class}, new Object[]{name});
    }

    public static Object bean(Object context, Class<?> type) {
        return Reflects.invokeQuiet(context, "getBean", new Class[]{Class.class}, new Object[]{type});
    }

    public static String contextName(Object context) {
        Object id = Reflects.invokeQuiet(context, "getId", null, null);
        return context.getClass().getName() + (id == null ? "" : "(" + id + ")");
    }

    public static RouteEntry entry(String type, Object context, String name, List<String> routes, Class<?> clazz) {
        RouteEntry entry = new RouteEntry();
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
}
