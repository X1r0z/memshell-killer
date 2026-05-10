package memshell.killer.core;

import memshell.killer.inspect.ClassInfo;

import java.util.ArrayList;
import java.util.List;

public class DumpResult {
    public String type;
    public String context;
    public List<String> routes = new ArrayList<>();
    public String name;
    public String className;
    public ClassInfo classInfo;
}
