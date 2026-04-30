package memshell.killer;

import memshell.killer.route.RemoveResult;
import memshell.killer.route.spring.SpringControllerHandler;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class SpringControllerHandlerTest {
    @Test
    public void removeRequestMappingUsesUnregisterMapping() {
        RequestMapping mapping = new RequestMapping();
        Object key = "requestMappingInfo";
        mapping.handlerMethods.put(key, new HandlerMethod(BadController.class));

        RemoveResult result = handler(mapping).remove(BadController.class.getName());

        assertEquals(1, result.removed);
        assertEquals(1, mapping.unregisterMappingCalls);
        assertFalse(mapping.handlerMethods.containsKey(key));
    }

    @Test
    public void removeHandlerMapPrefersFrameworkUnregisterHandler() {
        UrlHandlerMapping mapping = new UrlHandlerMapping();
        mapping.handlerMap.put("/shell", new BadController());

        RemoveResult result = handler(mapping).remove(BadController.class.getName());

        assertEquals(1, result.removed);
        assertEquals(1, mapping.unregisterHandlerCalls);
        assertFalse(mapping.handlerMap.containsKey("/shell"));
    }

    @Test
    public void removeHandlerMapFallsBackToFieldRemoval() {
        FieldOnlyMapping mapping = new FieldOnlyMapping();
        mapping.handlerMap.put("/shell", new BadController());

        RemoveResult result = handler(mapping).remove(BadController.class.getName());

        assertEquals(1, result.removed);
        assertFalse(mapping.handlerMap.containsKey("/shell"));
    }

    private TestHandler handler(Object mapping) {
        return new TestHandler(Arrays.asList(mapping));
    }

    static class TestHandler extends SpringControllerHandler {
        private final List<Object> mappings;

        TestHandler(List<Object> mappings) {
            this.mappings = mappings;
        }

        @Override
        protected List<Object> handlerMappings() {
            return mappings;
        }
    }

    static class RequestMapping {
        Map<Object, Object> handlerMethods = new HashMap<Object, Object>();
        int unregisterMappingCalls;

        Map<Object, Object> getHandlerMethods() {
            return handlerMethods;
        }

        void unregisterMapping(Object key) {
            unregisterMappingCalls++;
            handlerMethods.remove(key);
        }
    }

    static class UrlHandlerMapping {
        Map<Object, Object> handlerMap = new HashMap<Object, Object>();
        int unregisterHandlerCalls;

        void unregisterHandler(String path) {
            unregisterHandlerCalls++;
            handlerMap.remove(path);
        }
    }

    static class FieldOnlyMapping {
        Map<Object, Object> handlerMap = new HashMap<Object, Object>();
    }

    static class HandlerMethod {
        private final Class<?> beanType;

        HandlerMethod(Class<?> beanType) {
            this.beanType = beanType;
        }

        Class<?> getBeanType() {
            return beanType;
        }
    }

    static class BadController {
    }
}
