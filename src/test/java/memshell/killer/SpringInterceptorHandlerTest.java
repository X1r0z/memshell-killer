package memshell.killer;

import memshell.killer.route.RemoveResult;
import memshell.killer.route.RouteEntry;
import memshell.killer.route.spring.SpringInterceptorHandler;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SpringInterceptorHandlerTest {
    @Test
    public void dumpUnwrapsMappedInterceptorAndRoutes() {
        Mapping mapping = new Mapping();
        mapping.mappedInterceptors.add(new FakeMappedInterceptor(new BadInterceptor(), Arrays.asList("/shell/**")));

        List<RouteEntry> routes = handler(mapping).dump();

        assertEquals(1, routes.size());
        assertEquals(BadInterceptor.class.getName(), routes.get(0).className);
        assertEquals(Arrays.asList("/shell/**"), routes.get(0).routes);
    }

    @Test
    public void removeCoversInterceptorsFieldAndWrappedClass() {
        Mapping mapping = new Mapping();
        mapping.interceptors.add(new BadInterceptor());
        mapping.mappedInterceptors.add(new FakeMappedInterceptor(new BadInterceptor(), Arrays.asList("/shell/**")));
        mapping.mappedInterceptors.add(new FakeMappedInterceptor(new SafeInterceptor(), Arrays.asList("/safe/**")));

        RemoveResult result = handler(mapping).remove(BadInterceptor.class.getName());

        assertEquals(2, result.removed);
        assertEquals(0, mapping.interceptors.size());
        assertEquals(1, mapping.mappedInterceptors.size());
        assertTrue(mapping.mappedInterceptors.get(0).getInterceptor() instanceof SafeInterceptor);
    }

    private TestHandler handler(Object mapping) {
        return new TestHandler(Arrays.asList(mapping));
    }

    static class TestHandler extends SpringInterceptorHandler {
        private final List<Object> mappings;

        TestHandler(List<Object> mappings) {
            this.mappings = mappings;
        }

        @Override
        protected List<Object> handlerMappings() {
            return mappings;
        }
    }

    static class Mapping {
        List<Object> adaptedInterceptors = new ArrayList<Object>();
        List<Object> interceptors = new ArrayList<Object>();
        List<FakeMappedInterceptor> mappedInterceptors = new ArrayList<FakeMappedInterceptor>();
    }

    static class FakeMappedInterceptor {
        private final Object interceptor;
        private final List<String> pathPatterns;

        FakeMappedInterceptor(Object interceptor, List<String> pathPatterns) {
            this.interceptor = interceptor;
            this.pathPatterns = pathPatterns;
        }

        Object getInterceptor() {
            return interceptor;
        }

        List<String> getPathPatterns() {
            return pathPatterns;
        }
    }

    static class BadInterceptor {
    }

    static class SafeInterceptor {
    }
}
