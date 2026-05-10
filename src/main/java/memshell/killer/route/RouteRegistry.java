package memshell.killer.route;

import memshell.killer.route.spring.SpringControllerHandler;
import memshell.killer.route.spring.SpringInterceptorHandler;
import memshell.killer.route.tomcat.TomcatFilterHandler;
import memshell.killer.route.tomcat.TomcatListenerHandler;
import memshell.killer.route.tomcat.TomcatServletHandler;
import memshell.killer.route.tomcat.TomcatValveHandler;

import java.util.HashMap;
import java.util.Map;

public class RouteRegistry {
    private final Map<String, RouteHandler> handlers = new HashMap<>();

    public RouteRegistry() {
        handlers.put(RouteType.FILTER, new TomcatFilterHandler());
        handlers.put(RouteType.LISTENER, new TomcatListenerHandler());
        handlers.put(RouteType.VALVE, new TomcatValveHandler());
        handlers.put(RouteType.SERVLET, new TomcatServletHandler());
        handlers.put(RouteType.CONTROLLER, new SpringControllerHandler());
        handlers.put(RouteType.INTERCEPTOR, new SpringInterceptorHandler());
    }

    public RouteHandler get(String type) {
        RouteHandler handler = handlers.get(type);
        if (handler == null) {
            throw new IllegalArgumentException("unsupported type: " + type);
        }
        return handler;
    }
}
