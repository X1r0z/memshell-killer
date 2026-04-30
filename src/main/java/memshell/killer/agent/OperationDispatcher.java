package memshell.killer.agent;

import memshell.killer.core.OperationRequest;
import memshell.killer.core.OperationResponse;
import memshell.killer.core.RouteType;
import memshell.killer.route.RemoveResult;
import memshell.killer.route.RouteEntry;
import memshell.killer.route.RouteHandler;

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OperationDispatcher {
    private final Instrumentation instrumentation;
    private final HandlerRegistry registry = new HandlerRegistry();

    public OperationDispatcher(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
    }

    public OperationResponse dispatch(OperationRequest request) {
        try {
            if ("dump".equals(request.command)) {
                return OperationResponse.ok(dump(request.type));
            }
            if ("remove".equals(request.command)) {
                RouteHandler handler = registry.get(request.type);
                return OperationResponse.ok(handler.remove(request.className));
            }
            if ("jad".equals(request.command)) {
                return OperationResponse.ok(new JadService(instrumentation).decompile(request.className, request.method));
            }
            if ("call".equals(request.command)) {
                return OperationResponse.ok(new CallGraphService(instrumentation).analyze(request.className));
            }
            return OperationResponse.error("unknown command: " + request.command);
        } catch (Throwable t) {
            return OperationResponse.error(stackTrace(t));
        }
    }

    private Map<String, Object> dump(String requestedType) {
        Map<String, Object> data = new LinkedHashMap<>();
        List<String> types = requestedType == null || "all".equals(requestedType) ? RouteType.all() : java.util.Collections.singletonList(requestedType);
        for (String type : types) {
            try {
                RouteHandler handler = registry.get(type);
                List<RouteEntry> routes = dedupe(handler.dump());
                data.put(type, routes);
            } catch (Throwable t) {
                Map<String, Object> error = new LinkedHashMap<>();
                error.put("routes", new ArrayList<RemoveResult>());
                error.put("error", stackTrace(t));
                data.put(type, error);
            }
        }
        return data;
    }

    private List<RouteEntry> dedupe(List<RouteEntry> entries) {
        List<RouteEntry> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (RouteEntry entry : entries) {
            String key = entry.type + ":" + entry.className + ":" + entry.routes + ":" + entry.context;
            if (seen.add(key)) {
                result.add(entry);
            }
        }
        return result;
    }

    private static String stackTrace(Throwable throwable) {
        java.io.StringWriter sw = new java.io.StringWriter();
        throwable.printStackTrace(new java.io.PrintWriter(sw));
        return sw.toString();
    }
}
