package memshell.killer;

import memshell.killer.route.RemoveResult;
import memshell.killer.route.RouteEntry;
import memshell.killer.route.tomcat.TomcatFilterHandler;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class TomcatFilterHandlerTest {
    @Test
    public void dumpUsesFilterDefWhenFilterConfigHasNoInstance() {
        FilterContext context = new FilterContext();

        List<RouteEntry> routes = handler(context).dump();

        assertEquals(1, routes.size());
        assertEquals(UninitializedFilter.class.getName(), routes.get(0).className);
        assertEquals(Arrays.asList("/shell/*"), routes.get(0).routes);
    }

    @Test
    public void removeUninitializedFilterByFilterDefClassName() throws Exception {
        FilterContext context = new FilterContext();

        RemoveResult result = handler(context).remove(UninitializedFilter.class.getName());

        assertEquals(1, result.removed);
        assertEquals(0, context.filterMaps.length);
        assertEquals(0, context.filterDefs.size());
    }

    private TestHandler handler(Object context) {
        return new TestHandler(Arrays.asList(context));
    }

    static class TestHandler extends TomcatFilterHandler {
        private final Iterable<Object> contexts;

        TestHandler(Iterable<Object> contexts) {
            this.contexts = contexts;
        }

        @Override
        protected Iterable<Object> contexts() {
            return contexts;
        }
    }

    static class FilterContext {
        Map<Object, Object> filterConfigs = new HashMap<Object, Object>();
        Map<String, FilterDef> filterDefs = new HashMap<String, FilterDef>();
        FilterMap[] filterMaps = {new FilterMap()};

        FilterContext() {
            filterDefs.put("shellFilter", new FilterDef());
        }

        Object[] findFilterMaps() {
            return filterMaps;
        }

        Object findFilterConfig(String name) {
            return filterConfigs.get(name);
        }

        Object findFilterDef(String name) {
            return filterDefs.get(name);
        }

        void removeFilterDef(Object filterDef) {
            filterDefs.values().remove(filterDef);
        }

        void removeFilterMap(Object filterMap) {
            filterMaps = new FilterMap[0];
        }
    }

    static class FilterDef {
        String getFilterClass() {
            return UninitializedFilter.class.getName();
        }
    }

    static class FilterMap {
        String getFilterName() {
            return "shellFilter";
        }

        String getURLPattern() {
            return "/shell/*";
        }
    }

    static class UninitializedFilter {
    }
}
