package memshell.killer.route.tomcat;

import memshell.killer.core.RouteType;
import memshell.killer.route.RemoveResult;
import memshell.killer.route.RouteEntry;
import memshell.killer.route.RouteHandler;
import memshell.killer.util.Reflects;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TomcatServletHandler implements RouteHandler {
    @Override
    public String type() {
        return RouteType.SERVLET;
    }

    @Override
    public List<RouteEntry> dump() {
        List<RouteEntry> entries = new ArrayList<>();
        for (Object context : contexts()) {
            Map<String, List<String>> routes = servletRoutes(context);
            Object children = Reflects.getQuiet(context, "children");
            if (children instanceof Map) {
                for (Map.Entry<?, ?> child : ((Map<?, ?>) children).entrySet()) {
                    Object wrapper = child.getValue();
                    String name = String.valueOf(child.getKey());
                    String servletClass = stringValue(Reflects.invokeQuiet(wrapper, "getServletClass", null, null));
                    if (servletClass == null) {
                        servletClass = stringValue(Reflects.getQuiet(wrapper, "servletClass"));
                    }
                    Object servlet = Reflects.getQuiet(wrapper, "instance");
                    if (servlet == null) {
                        servlet = Reflects.invokeAnyQuiet(wrapper, "getServlet");
                    }
                    Class<?> clazz = TomcatSupport.classFromObjectOrName(servlet, servletClass, TomcatSupport.webAppClassLoader(context));
                    entries.add(TomcatSupport.entry(type(), context, name, routes.get(name), clazz));
                }
            }
        }
        return entries;
    }

    @Override
    public RemoveResult remove(String className) throws Exception {
        RemoveResult result = new RemoveResult();
        result.type = type();
        result.className = className;
        for (Object context : contexts()) {
            Object children = Reflects.getQuiet(context, "children");
            if (!(children instanceof Map)) {
                continue;
            }
            List<String> names = new ArrayList<>();
            for (Map.Entry<?, ?> child : ((Map<?, ?>) children).entrySet()) {
                Object wrapper = child.getValue();
                Object servlet = Reflects.getQuiet(wrapper, "instance");
                if (servlet == null) {
                    servlet = Reflects.invokeAnyQuiet(wrapper, "getServlet");
                }
                String servletClass = stringValue(Reflects.invokeQuiet(wrapper, "getServletClass", null, null));
                if (servletClass == null) {
                    servletClass = stringValue(Reflects.getQuiet(wrapper, "servletClass"));
                }
                if ((servlet != null && servlet.getClass().getName().equals(className)) || className.equals(servletClass)) {
                    names.add(String.valueOf(child.getKey()));
                }
            }
            for (String name : names) {
                Object wrapper = ((Map<?, ?>) children).get(name);
                removeServletMappings(context, name);
                Reflects.invokeAnyQuiet(context, "removeChild", wrapper);
                ((Map<?, ?>) children).remove(name);
                result.removed++;
                result.details.add(TomcatSupport.contextName(context) + " servlet " + name);
            }
        }
        return result;
    }

    private Map<String, List<String>> servletRoutes(Object context) {
        Map<String, List<String>> routes = new LinkedHashMap<>();
        Object mappings = Reflects.invokeQuiet(context, "findServletMappings", null, null);
        for (String pattern : TomcatSupport.stringList(mappings)) {
            Object name = Reflects.invokeQuiet(context, "findServletMapping", new Class[]{String.class}, new Object[]{pattern});
            if (name != null) {
                if (!routes.containsKey(String.valueOf(name))) {
                    routes.put(String.valueOf(name), new ArrayList<>());
                }
                routes.get(String.valueOf(name)).add(pattern);
            }
        }
        if (routes.isEmpty()) {
            Object fieldMappings = Reflects.getQuiet(context, "servletMappings");
            if (fieldMappings instanceof Map) {
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) fieldMappings).entrySet()) {
                    String name = String.valueOf(entry.getValue());
                    if (!routes.containsKey(name)) {
                        routes.put(name, new ArrayList<>());
                    }
                    routes.get(name).add(String.valueOf(entry.getKey()));
                }
            }
        }
        return routes;
    }

    protected Iterable<Object> contexts() {
        return new TomcatContextFinder().find();
    }

    private void removeServletMappings(Object context, String name) {
        Object mappings = Reflects.invokeQuiet(context, "findServletMappings", null, null);
        for (String pattern : TomcatSupport.stringList(mappings)) {
            Object mappedName = Reflects.invokeQuiet(context, "findServletMapping", new Class[]{String.class}, new Object[]{pattern});
            if (name.equals(String.valueOf(mappedName))) {
                Reflects.invokeQuiet(context, "removeServletMapping", new Class[]{String.class}, new Object[]{pattern});
                Reflects.invokeQuiet(context, "removeServletMappingDecoded", new Class[]{String.class}, new Object[]{pattern});
            }
        }
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
