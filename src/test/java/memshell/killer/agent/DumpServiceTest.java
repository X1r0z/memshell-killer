package memshell.killer.agent;

import memshell.killer.core.DumpResult;
import memshell.killer.core.RemoveResult;
import memshell.killer.route.RouteHandler;
import memshell.killer.route.RouteRegistry;
import memshell.killer.route.RouteType;
import memshell.killer.service.DumpService;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DumpServiceTest {
    @Test
    public void dumpDedupesRoutesPerType() throws Exception {
        FakeRegistry registry = new FakeRegistry();
        registry.put(RouteType.FILTER, new StaticHandler(entry("BadFilter", "/x"), entry("BadFilter", "/x"), entry("OtherFilter", "/y")));

        List<DumpResult> routes = new DumpService(registry).dump(RouteType.FILTER);

        Assert.assertEquals(2, routes.size());
        Assert.assertEquals(RouteType.FILTER, routes.get(0).type);
        Assert.assertEquals("BadFilter", routes.get(0).className);
        Assert.assertEquals("OtherFilter", routes.get(1).className);
    }

    @Test(expected = Exception.class)
    public void dumpFailsWhenHandlerFails() throws Exception {
        FakeRegistry registry = new FakeRegistry();
        registry.put(RouteType.FILTER, new ThrowingHandler());

        new DumpService(registry).dump(RouteType.FILTER);
    }

    private static DumpResult entry(String className, String route) {
        DumpResult entry = new DumpResult();
        entry.type = RouteType.FILTER;
        entry.context = "/app";
        entry.routes.add(route);
        entry.className = className;
        return entry;
    }

    private static class FakeRegistry extends RouteRegistry {
        private final Map<String, RouteHandler> handlers = new HashMap<>();

        void put(String type, RouteHandler handler) {
            handlers.put(type, handler);
        }

        @Override
        public RouteHandler get(String type) {
            RouteHandler handler = handlers.get(type);
            if (handler == null) {
                throw new IllegalArgumentException("unsupported type: " + type);
            }
            return handler;
        }
    }

    private static class StaticHandler implements RouteHandler {
        private final List<DumpResult> entries;

        StaticHandler(DumpResult... entries) {
            this.entries = Arrays.asList(entries);
        }

        @Override
        public String type() {
            return RouteType.FILTER;
        }

        @Override
        public List<DumpResult> dump() throws Exception {
            return entries;
        }

        @Override
        public RemoveResult remove(String className) {
            return null;
        }
    }

    private static class ThrowingHandler extends StaticHandler {
        ThrowingHandler() {
            super();
        }

        @Override
        public List<DumpResult> dump() throws Exception {
            throw new Exception("boom");
        }
    }
}
