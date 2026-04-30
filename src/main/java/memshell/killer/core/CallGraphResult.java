package memshell.killer.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CallGraphResult {
    public String className;
    public Map<String, List<String>> edges = new LinkedHashMap<>();
    public List<String> chains = new ArrayList<>();
}
