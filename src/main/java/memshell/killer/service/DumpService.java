package memshell.killer.service;

import memshell.killer.core.DumpResult;
import memshell.killer.route.RouteHandler;
import memshell.killer.route.RouteRegistry;
import memshell.killer.route.RouteType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DumpService {
    private final RouteRegistry registry;

    public DumpService(RouteRegistry registry) {
        this.registry = registry;
    }

    public List<DumpResult> dump(String requestedType) throws Exception {
        List<DumpResult> result = new ArrayList<>();
        List<String> types = requestedType == null || "all".equals(requestedType) ? RouteType.all() : Collections.singletonList(requestedType);
        for (String type : types) {
            RouteHandler handler = registry.get(type);
            result.addAll(dedupe(handler.dump()));
        }
        return result;
    }

    private List<DumpResult> dedupe(List<DumpResult> entries) {
        List<DumpResult> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (DumpResult entry : entries) {
            String key = entry.type + ":" + entry.className + ":" + entry.routes + ":" + entry.context;
            if (seen.add(key)) {
                result.add(entry);
            }
        }
        return result;
    }

}
