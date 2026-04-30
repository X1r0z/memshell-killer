package memshell.killer.route.tomcat;

import memshell.killer.core.RouteType;
import memshell.killer.route.RemoveResult;
import memshell.killer.route.RouteEntry;
import memshell.killer.route.RouteHandler;
import memshell.killer.util.Reflects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TomcatValveHandler implements RouteHandler {
    @Override
    public String type() {
        return RouteType.VALVE;
    }

    @Override
    public List<RouteEntry> dump() {
        List<RouteEntry> entries = new ArrayList<>();
        for (Object context : new TomcatContextFinder().find()) {
            Object pipeline = Reflects.invokeQuiet(context, "getPipeline", null, null);
            Object valves = pipeline == null ? null : Reflects.invokeQuiet(pipeline, "getValves", null, null);
            for (Object valve : Reflects.asList(valves)) {
                if (valve != null) {
                    entries.add(TomcatSupport.entry(type(), context, valve.getClass().getName(), Collections.singletonList("/*"), valve.getClass()));
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
        for (Object context : new TomcatContextFinder().find()) {
            Object pipeline = Reflects.invokeQuiet(context, "getPipeline", null, null);
            Object valves = pipeline == null ? null : Reflects.invokeQuiet(pipeline, "getValves", null, null);
            for (Object valve : Reflects.asList(valves)) {
                if (valve != null && valve.getClass().getName().equals(className)) {
                    Class<?> valveType = valve.getClass().getClassLoader().loadClass("org.apache.catalina.Valve");
                    Reflects.invokeQuiet(pipeline, "removeValve", new Class[]{valveType}, new Object[]{valve});
                    result.removed++;
                    result.details.add(TomcatSupport.contextName(context) + " valve " + className);
                }
            }
        }
        return result;
    }
}
