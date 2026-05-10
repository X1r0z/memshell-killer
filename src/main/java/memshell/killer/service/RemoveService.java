package memshell.killer.service;

import memshell.killer.core.RemoveResult;
import memshell.killer.route.RouteHandler;
import memshell.killer.route.RouteRegistry;

public class RemoveService {
    private final RouteRegistry registry;

    public RemoveService(RouteRegistry registry) {
        this.registry = registry;
    }

    public RemoveResult remove(String type, String className) throws Exception {
        RouteHandler handler = registry.get(type);
        return handler.remove(className);
    }
}
