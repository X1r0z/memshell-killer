package memshell.killer.agent;

import memshell.killer.core.RemoveResult;
import memshell.killer.core.DumpResult;
import memshell.killer.route.RouteHandler;
import memshell.killer.route.RouteRegistry;
import memshell.killer.route.RouteType;
import memshell.killer.service.RemoveService;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RemoveServiceTest {
    @Test
    public void removeDelegatesToRequestedTypeHandler() throws Exception {
        FakeRegistry registry = new FakeRegistry();
        CapturingHandler handler = new CapturingHandler();
        registry.put(RouteType.FILTER, handler);

        RemoveResult result = new RemoveService(registry).remove(RouteType.FILTER, "BadFilter");

        Assert.assertEquals("BadFilter", handler.className);
        Assert.assertEquals(RouteType.FILTER, result.type);
        Assert.assertEquals("BadFilter", result.className);
        Assert.assertEquals(1, result.removed);
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

    private static class CapturingHandler implements RouteHandler {
        private String className;

        @Override
        public String type() {
            return RouteType.FILTER;
        }

        @Override
        public List<DumpResult> dump() {
            return null;
        }

        @Override
        public RemoveResult remove(String className) {
            this.className = className;
            RemoveResult result = new RemoveResult();
            result.type = RouteType.FILTER;
            result.className = className;
            result.removed = 1;
            return result;
        }
    }
}
