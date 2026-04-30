package memshell.killer.route;

import memshell.killer.core.ClassMetadata;

import java.util.ArrayList;
import java.util.List;

public class RouteEntry {
    public String type;
    public String context;
    public List<String> routes = new ArrayList<>();
    public String name;
    public String className;
    public ClassMetadata classInfo;
}
