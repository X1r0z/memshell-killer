package memshell.killer.route.tomcat;

import memshell.killer.route.RouteType;
import memshell.killer.core.RemoveResult;
import memshell.killer.core.DumpResult;
import memshell.killer.route.RouteHandler;
import memshell.killer.util.Reflects;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TomcatFilterHandler implements RouteHandler {
    @Override
    public String type() {
        return RouteType.FILTER;
    }

    @Override
    public List<DumpResult> dump() {
        List<DumpResult> entries = new ArrayList<>();
        for (Object context : contexts()) {
            Map<String, List<String>> routes = filterRoutes(context);
            Map<?, ?> configs = mapField(context, "filterConfigs");
            Map<?, ?> defs = mapField(context, "filterDefs");
            for (Object key : mergedKeys(configs, defs, routes)) {
                String name = String.valueOf(key);
                Object config = configs.get(key);
                Object def = defs.get(key);
                Object filter = Reflects.getQuiet(config, "filter");
                String className = stringValue(Reflects.invokeQuiet(def, "getFilterClass", null, null));
                if (className == null) {
                    className = stringValue(Reflects.getQuiet(def, "filterClass"));
                }
                Class<?> clazz = TomcatSupport.classFromObjectOrName(filter, className, TomcatSupport.webAppClassLoader(context));
                entries.add(TomcatSupport.entry(type(), context, name, routes.get(name), clazz));
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
            Map<?, ?> configs = mapField(context, "filterConfigs");
            Map<?, ?> defs = mapField(context, "filterDefs");
            List<String> names = new ArrayList<>();
            for (Object key : mergedKeys(configs, defs, new LinkedHashMap<>())) {
                Object config = configs.get(key);
                Object def = defs.get(key);
                Object filter = Reflects.getQuiet(config, "filter");
                String defClass = stringValue(Reflects.invokeQuiet(def, "getFilterClass", null, null));
                if (defClass == null) {
                    defClass = stringValue(Reflects.getQuiet(def, "filterClass"));
                }
                if ((filter != null && filter.getClass().getName().equals(className)) || className.equals(defClass)) {
                    names.add(String.valueOf(key));
                }
            }
            for (String name : names) {
                Object def = defs.get(name);
                configs.remove(name);
                defs.remove(name);
                if (def != null) {
                    Reflects.invokeAnyQuiet(context, "removeFilterDef", def);
                }
                removeFilterMaps(context, name);
                result.removed++;
                result.details.add(TomcatSupport.contextName(context) + " filter " + name);
            }
        }
        return result;
    }

    private Map<String, List<String>> filterRoutes(Object context) {
        Map<String, List<String>> routes = new LinkedHashMap<>();
        Object maps = Reflects.invokeQuiet(context, "findFilterMaps", null, null);
        if (maps == null) {
            Object holder = Reflects.getQuiet(context, "filterMaps");
            maps = Reflects.getQuiet(holder, "array");
            if (maps == null) {
                maps = holder;
            }
        }
        for (Object map : Reflects.asList(maps)) {
            Object name = Reflects.invokeQuiet(map, "getFilterName", null, null);
            if (name == null) {
                name = Reflects.getQuiet(map, "filterName");
            }
            if (name != null) {
                routes.put(String.valueOf(name), TomcatSupport.filterMapRoutes(map));
            }
        }
        return routes;
    }

    protected Iterable<Object> contexts() {
        return new TomcatContextFinder().find();
    }

    private void removeFilterMaps(Object context, String filterName) throws Exception {
        Object liveMaps = Reflects.invokeQuiet(context, "findFilterMaps", null, null);
        for (Object map : Reflects.asList(liveMaps)) {
            Object name = Reflects.invokeQuiet(map, "getFilterName", null, null);
            if (name == null) {
                name = Reflects.getQuiet(map, "filterName");
            }
            if (filterName.equals(String.valueOf(name))) {
                Reflects.invokeAnyQuiet(context, "removeFilterMap", map);
            }
        }
        Object holder = Reflects.getQuiet(context, "filterMaps");
        Object maps = Reflects.getQuiet(holder, "array");
        Object target = maps == null ? holder : maps;
        if (target != null && target.getClass().isArray()) {
            List<Object> kept = new ArrayList<>();
            Class<?> component = target.getClass().getComponentType();
            for (Object map : Reflects.asList(target)) {
                Object name = Reflects.invokeQuiet(map, "getFilterName", null, null);
                if (name == null) {
                    name = Reflects.getQuiet(map, "filterName");
                }
                if (!filterName.equals(String.valueOf(name))) {
                    kept.add(map);
                }
            }
            Object array = Array.newInstance(component, kept.size());
            for (int i = 0; i < kept.size(); i++) {
                Array.set(array, i, kept.get(i));
            }
            if (maps == null) {
                Reflects.set(context, "filterMaps", array);
            } else {
                Reflects.set(holder, "array", array);
            }
        }
    }

    private Map<?, ?> mapField(Object context, String name) {
        Object value = Reflects.getQuiet(context, name);
        return value instanceof Map ? (Map<?, ?>) value : new LinkedHashMap<>();
    }

    private List<Object> mergedKeys(Map<?, ?> a, Map<?, ?> b, Map<String, List<String>> c) {
        List<Object> keys = new ArrayList<>(a.keySet());
        for (Object key : b.keySet()) {
            if (!keys.contains(key)) keys.add(key);
        }
        for (String key : c.keySet()) {
            if (!keys.contains(key)) keys.add(key);
        }
        return keys;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
