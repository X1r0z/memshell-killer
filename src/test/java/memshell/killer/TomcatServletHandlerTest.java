package memshell.killer;

import memshell.killer.route.RemoveResult;
import memshell.killer.route.RouteEntry;
import memshell.killer.route.tomcat.TomcatServletHandler;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class TomcatServletHandlerTest {
    @Test
    public void dumpUsesServletClassAndServletMappingsFieldFallback() {
        ServletContext context = new ServletContext();

        List<RouteEntry> routes = handler(context).dump();

        assertEquals(1, routes.size());
        assertEquals(UninitializedServlet.class.getName(), routes.get(0).className);
        assertEquals(Arrays.asList("/shell"), routes.get(0).routes);
    }

    @Test
    public void removeUsesServletClassWhenInstanceMissing() throws Exception {
        ServletContext context = new ServletContext();

        RemoveResult result = handler(context).remove(UninitializedServlet.class.getName());

        assertEquals(1, result.removed);
        assertEquals(0, context.children.size());
    }

    private TestHandler handler(Object context) {
        return new TestHandler(Arrays.asList(context));
    }

    static class TestHandler extends TomcatServletHandler {
        private final Iterable<Object> contexts;

        TestHandler(Iterable<Object> contexts) {
            this.contexts = contexts;
        }

        @Override
        protected Iterable<Object> contexts() {
            return contexts;
        }
    }

    static class ServletContext {
        Map<String, Wrapper> children = new LinkedHashMap<String, Wrapper>();
        Map<String, String> servletMappings = new LinkedHashMap<String, String>();

        ServletContext() {
            children.put("shellServlet", new Wrapper());
            servletMappings.put("/shell", "shellServlet");
        }

        Object[] findServletMappings() {
            return new Object[0];
        }

        void removeChild(Object wrapper) {
            children.values().remove(wrapper);
        }
    }

    static class Wrapper {
        String servletClass = UninitializedServlet.class.getName();

        String getServletClass() {
            return servletClass;
        }

        String getName() {
            return "shellServlet";
        }
    }

    static class UninitializedServlet {
    }
}
