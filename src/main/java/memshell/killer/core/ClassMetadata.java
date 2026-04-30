package memshell.killer.core;

import java.util.ArrayList;
import java.util.List;

public class ClassMetadata {
    public String className;
    public String classLoader;
    public String superClass;
    public List<String> interfaces = new ArrayList<>();
    public List<String> fields = new ArrayList<>();
    public List<String> methods = new ArrayList<>();
}
