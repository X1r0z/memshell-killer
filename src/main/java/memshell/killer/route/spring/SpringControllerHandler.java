package memshell.killer.route.spring;

import memshell.killer.route.RouteType;
import memshell.killer.core.RemoveResult;
import memshell.killer.core.DumpResult;
import memshell.killer.route.RouteHandler;
import memshell.killer.util.Reflects;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public class SpringControllerHandler implements RouteHandler {
    @Override
    public String type() {
        return RouteType.CONTROLLER;
    }

    @Override
    public List<DumpResult> dump() {
        List<DumpResult> entries = new ArrayList<>();
        for (Object mapping : handlerMappings()) {
            dumpRequestMappingHandlerMapping(mapping, entries);
            dumpUrlHandlerMaps(mapping, entries);
        }
        return entries;
    }

    @Override
    public RemoveResult remove(String className) {
        RemoveResult result = new RemoveResult();
        result.type = type();
        result.className = className;
        for (Object mapping : handlerMappings()) {
            result.removed += removeRequestMappings(mapping, className, result);
            result.removed += removeUrlHandlers(mapping, className, result);
        }
        return result;
    }

    private void dumpRequestMappingHandlerMapping(Object mapping, List<DumpResult> entries) {
        Object handlerMethods = handlerMethods(mapping);
        if (handlerMethods instanceof Map) {
            for (Map.Entry<?, ?> item : ((Map<?, ?>) handlerMethods).entrySet()) {
                Object handlerMethod = item.getValue();
                Class<?> clazz = handlerClass(handlerMethod);
                String route = String.valueOf(item.getKey());
                String name = handlerName(handlerMethod);
                entries.add(SpringSupport.entry(type(), mapping, name, Collections.singletonList(route), clazz));
            }
        }
    }

    protected List<Object> handlerMappings() {
        return new SpringContextFinder().handlerMappings();
    }

    private void dumpUrlHandlerMaps(Object mapping, List<DumpResult> entries) {
        Object handlerMap = Reflects.getQuiet(mapping, "handlerMap");
        if (handlerMap instanceof Map) {
            for (Map.Entry<?, ?> item : ((Map<?, ?>) handlerMap).entrySet()) {
                Object handler = item.getValue();
                Class<?> clazz = handler == null ? null : handler.getClass();
                entries.add(SpringSupport.entry(type(), mapping, String.valueOf(item.getKey()), mappingPaths(item.getKey()), clazz));
            }
        }
    }

    private int removeRequestMappings(Object mapping, String className, RemoveResult result) {
        int removed = 0;
        Object handlerMethods = handlerMethods(mapping);
        if (handlerMethods instanceof Map) {
            List<Object> keys = new ArrayList<>();
            for (Map.Entry<?, ?> item : ((Map<?, ?>) handlerMethods).entrySet()) {
                Class<?> clazz = handlerClass(item.getValue());
                if (clazz != null && clazz.getName().equals(className)) {
                    keys.add(item.getKey());
                }
            }
            for (Object key : keys) {
                Reflects.invokeAnyQuiet(mapping, "unregisterMapping", key);
                ((Map<?, ?>) handlerMethods).remove(key);
                removed++;
                result.details.add(SpringSupport.contextName(mapping) + " controller " + key);
            }
        }
        return removed;
    }

    private int removeUrlHandlers(Object mapping, String className, RemoveResult result) {
        int removed = 0;
        Object handlerMap = Reflects.getQuiet(mapping, "handlerMap");
        if (handlerMap instanceof Map) {
            List<Object> keys = new ArrayList<>();
            for (Map.Entry<?, ?> item : ((Map<?, ?>) handlerMap).entrySet()) {
                Object handler = item.getValue();
                if (handler != null && handler.getClass().getName().equals(className)) {
                    keys.add(item.getKey());
                }
            }
            for (Object key : keys) {
                unregisterUrlHandler(mapping, (Map<?, ?>) handlerMap, key);
                ((Map<?, ?>) handlerMap).remove(key);
                removed++;
                result.details.add(SpringSupport.contextName(mapping) + " handlerMap " + key);
            }
        }
        return removed;
    }

    private Object handlerMethods(Object mapping) {
        Object handlerMethods = Reflects.invokeAnyQuiet(mapping, "getHandlerMethods");
        if (!(handlerMethods instanceof Map)) {
            handlerMethods = Reflects.getQuiet(mapping, "handlerMethods");
        }
        if (!(handlerMethods instanceof Map)) {
            Object registry = Reflects.getQuiet(mapping, "mappingRegistry");
            handlerMethods = Reflects.getQuiet(registry, "mappingLookup");
        }
        return handlerMethods;
    }

    private void unregisterUrlHandler(Object mapping, Map<?, ?> handlerMap, Object key) {
        int before = handlerMap.size();
        for (String path : mappingPaths(key)) {
            Reflects.invokeAnyQuiet(mapping, "unregisterHandler", path);
            if (!handlerMap.containsKey(key) || handlerMap.size() < before) {
                return;
            }
            Reflects.invokeAnyQuiet(mapping, "unregisterHandler", (Object) new String[]{path});
            if (!handlerMap.containsKey(key) || handlerMap.size() < before) {
                return;
            }
        }
        Reflects.invokeAnyQuiet(mapping, "unregisterMapping", key);
    }

    private List<String> mappingPaths(Object key) {
        if (key == null) {
            return Collections.emptyList();
        }
        if (key instanceof String) {
            return Collections.singletonList((String) key);
        }
        if (key instanceof String[]) {
            return Arrays.asList((String[]) key);
        }
        if (key instanceof Collection) {
            LinkedHashSet<String> result = new LinkedHashSet<>();
            for (Object item : (Collection<?>) key) {
                if (item != null && !String.valueOf(item).trim().isEmpty()) {
                    result.add(String.valueOf(item));
                }
            }
            return new ArrayList<>(result);
        }
        return Collections.singletonList(String.valueOf(key));
    }

    private Class<?> handlerClass(Object handlerMethod) {
        if (handlerMethod == null) {
            return null;
        }
        Object beanType = Reflects.invokeQuiet(handlerMethod, "getBeanType", null, null);
        if (beanType instanceof Class) {
            return (Class<?>) beanType;
        }
        Object bean = Reflects.invokeQuiet(handlerMethod, "getBean", null, null);
        return bean == null ? handlerMethod.getClass() : bean.getClass();
    }

    private String handlerName(Object handlerMethod) {
        if (handlerMethod == null) {
            return null;
        }
        Object method = Reflects.invokeQuiet(handlerMethod, "getMethod", null, null);
        if (method instanceof Method) {
            return ((Method) method).getName();
        }
        return handlerMethod.toString();
    }
}
