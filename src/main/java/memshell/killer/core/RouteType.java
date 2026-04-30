package memshell.killer.core;

import java.util.Arrays;
import java.util.List;

public final class RouteType {
    public static final String FILTER = "filter";
    public static final String LISTENER = "listener";
    public static final String VALVE = "valve";
    public static final String SERVLET = "servlet";
    public static final String CONTROLLER = "controller";
    public static final String INTERCEPTOR = "interceptor";

    private RouteType() {
    }

    public static List<String> all() {
        return Arrays.asList(FILTER, LISTENER, VALVE, SERVLET, CONTROLLER, INTERCEPTOR);
    }
}
