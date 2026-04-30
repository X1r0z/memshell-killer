package memshell.killer.route.spring;

import memshell.killer.core.RouteType;
import memshell.killer.route.RemoveResult;
import memshell.killer.route.RouteEntry;
import memshell.killer.route.RouteHandler;
import memshell.killer.util.Reflects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SpringInterceptorHandler implements RouteHandler {
    @Override
    public String type() {
        return RouteType.INTERCEPTOR;
    }

    @Override
    public List<RouteEntry> dump() {
        List<RouteEntry> entries = new ArrayList<>();
        for (Object mapping : handlerMappings()) {
            addInterceptors(mapping, entries, Reflects.getQuiet(mapping, "adaptedInterceptors"));
            addInterceptors(mapping, entries, Reflects.getQuiet(mapping, "interceptors"));
            addInterceptors(mapping, entries, Reflects.getQuiet(mapping, "mappedInterceptors"));
        }
        return entries;
    }

    @Override
    public RemoveResult remove(String className) {
        RemoveResult result = new RemoveResult();
        result.type = type();
        result.className = className;
        for (Object mapping : handlerMappings()) {
            result.removed += removeFromList(mapping, Reflects.getQuiet(mapping, "adaptedInterceptors"), className, result, "adaptedInterceptors");
            result.removed += removeFromList(mapping, Reflects.getQuiet(mapping, "interceptors"), className, result, "interceptors");
            result.removed += removeFromList(mapping, Reflects.getQuiet(mapping, "mappedInterceptors"), className, result, "mappedInterceptors");
        }
        return result;
    }

    private void addInterceptors(Object context, List<RouteEntry> entries, Object value) {
        for (Object item : Reflects.asList(value)) {
            Object interceptor = unwrapMappedInterceptor(item);
            if (interceptor != null) {
                entries.add(SpringSupport.entry(type(), context, interceptor.getClass().getName(), routes(item), interceptor.getClass()));
            }
        }
    }

    private int removeFromList(Object context, Object value, String className, RemoveResult result, String source) {
        if (!(value instanceof List)) {
            return 0;
        }
        int removed = 0;
        List list = (List) value;
        for (int i = list.size() - 1; i >= 0; i--) {
            Object item = list.get(i);
            Object interceptor = unwrapMappedInterceptor(item);
            if ((item != null && item.getClass().getName().equals(className))
                    || (interceptor != null && interceptor.getClass().getName().equals(className))) {
                list.remove(i);
                removed++;
            }
        }
        if (removed > 0) {
            result.details.add(SpringSupport.contextName(context) + " " + source + " " + className);
        }
        return removed;
    }

    private Object unwrapMappedInterceptor(Object item) {
        if (item == null) {
            return null;
        }
        Object interceptor = Reflects.invokeAnyQuiet(item, "getInterceptor");
        if (interceptor == null) {
            interceptor = Reflects.getQuiet(item, "interceptor");
        }
        return interceptor == null ? item : interceptor;
    }

    private List<String> routes(Object item) {
        List<String> routes = new ArrayList<>();
        addRoutes(routes, Reflects.invokeAnyQuiet(item, "getPathPatterns"));
        addRoutes(routes, Reflects.getQuiet(item, "includePatterns"));
        addRoutes(routes, Reflects.getQuiet(item, "pathPatterns"));
        return routes.isEmpty() ? Collections.singletonList("/**") : routes;
    }

    private void addRoutes(List<String> routes, Object value) {
        for (Object item : Reflects.asList(value)) {
            if (item != null && !String.valueOf(item).trim().isEmpty() && !routes.contains(String.valueOf(item))) {
                routes.add(String.valueOf(item));
            }
        }
    }

    protected List<Object> handlerMappings() {
        return new SpringContextFinder().handlerMappings();
    }
}
