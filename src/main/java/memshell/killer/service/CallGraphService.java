package memshell.killer.service;

import memshell.killer.agent.ClassFileDumper;
import memshell.killer.core.CallGraphResult;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CallGraphService {
    private final Instrumentation instrumentation;

    public CallGraphService(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
    }

    public CallGraphResult analyze(String className) throws Exception {
        Class<?> clazz = findLoadedClass(className);
        byte[] bytes = ClassFileDumper.dump(instrumentation, clazz);
        return analyzeBytes(className, bytes);
    }

    private Class<?> findLoadedClass(String className) {
        for (Class<?> clazz : instrumentation.getAllLoadedClasses()) {
            if (clazz.getName().equals(className)) {
                return clazz;
            }
        }
        throw new IllegalArgumentException("class not loaded: " + className);
    }

    public static CallGraphResult analyzeBytes(String className, byte[] bytes) {
        ClassNode classNode = new ClassNode();
        new ClassReader(bytes).accept(classNode, 0);

        Map<String, List<String>> internalEdges = new LinkedHashMap<>();
        Map<String, String> labels = new LinkedHashMap<>();
        Set<String> declared = new HashSet<>();
        for (MethodNode method : classNode.methods) {
            if ((method.access & Opcodes.ACC_SYNTHETIC) == 0 && !isLifecycleMethod(method.name)) {
                String key = methodKey(method.name, method.desc);
                declared.add(key);
                labels.put(key, displayMethod(method.name, method.desc));
            }
        }
        for (MethodNode method : classNode.methods) {
            if ((method.access & Opcodes.ACC_SYNTHETIC) != 0 || isLifecycleMethod(method.name)) {
                continue;
            }
            String caller = methodKey(method.name, method.desc);
            List<String> callees = new ArrayList<>();
            for (AbstractInsnNode node = method.instructions.getFirst(); node != null; node = node.getNext()) {
                if (node instanceof MethodInsnNode) {
                    MethodInsnNode call = (MethodInsnNode) node;
                    if ("<init>".equals(call.name) || "<clinit>".equals(call.name)) {
                        continue;
                    }
                    String callee;
                    String callKey = methodKey(call.name, call.desc);
                    if (classNode.name.equals(call.owner) && declared.contains(callKey)) {
                        callee = callKey;
                    } else {
                        String field = previousFieldName(node, classNode.name);
                        callee = field == null ? call.owner.replace('/', '.') + "." + call.name : field + "." + call.name;
                    }
                    if (!callees.contains(callee)) {
                        callees.add(callee);
                    }
                }
            }
            internalEdges.put(caller, callees);
        }

        Map<String, List<String>> displayEdges = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> edge : internalEdges.entrySet()) {
            List<String> callees = new ArrayList<>();
            for (String callee : edge.getValue()) {
                callees.add(labels.getOrDefault(callee, callee));
            }
            displayEdges.put(labels.get(edge.getKey()), callees);
        }

        List<String> chains = new ArrayList<>();
        for (String caller : internalEdges.keySet()) {
            buildChains(display(caller, labels), caller, internalEdges, labels, new HashSet<>(), chains);
        }

        CallGraphResult result = new CallGraphResult();
        result.className = className;
        result.edges = displayEdges;
        result.chains = chains;
        return result;
    }

    private static void buildChains(String path, String current, Map<String, List<String>> edges, Map<String, String> labels, Set<String> visiting, List<String> chains) {
        if (!visiting.add(current)) {
            chains.add(path + " -> " + display(current, labels) + " (cycle)");
            return;
        }
        List<String> callees = edges.get(current);
        if (callees == null || callees.isEmpty()) {
            if (!path.equals(display(current, labels))) {
                chains.add(path);
            }
            visiting.remove(current);
            return;
        }
        for (String callee : callees) {
            if (edges.containsKey(callee)) {
                buildChains(path + " -> " + display(callee, labels), callee, edges, labels, visiting, chains);
            } else {
                chains.add(path + " -> " + callee);
            }
        }
        visiting.remove(current);
    }

    private static String previousFieldName(AbstractInsnNode node, String owner) {
        AbstractInsnNode previous = node.getPrevious();
        int steps = 0;
        while (previous != null && steps < 4) {
            if (previous instanceof FieldInsnNode) {
                FieldInsnNode field = (FieldInsnNode) previous;
                if (field.getOpcode() == Opcodes.GETFIELD && owner.equals(field.owner)) {
                    return field.name;
                }
            }
            previous = previous.getPrevious();
            steps++;
        }
        return null;
    }

    private static boolean isLifecycleMethod(String name) {
        return "<init>".equals(name) || "<clinit>".equals(name);
    }

    private static String methodKey(String name, String descriptor) {
        return name + descriptor;
    }

    private static String display(String key, Map<String, String> labels) {
        return labels.getOrDefault(key, key);
    }

    private static String displayMethod(String name, String descriptor) {
        Type methodType = Type.getMethodType(descriptor);
        StringBuilder sb = new StringBuilder(name).append('(');
        Type[] arguments = methodType.getArgumentTypes();
        for (int i = 0; i < arguments.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(typeName(arguments[i]));
        }
        sb.append("): ").append(typeName(methodType.getReturnType()));
        return sb.toString();
    }

    private static String typeName(Type type) {
        if (type.getSort() == Type.ARRAY) {
            return typeName(type.getElementType()) + repeat(type.getDimensions());
        }
        if (type.getSort() == Type.OBJECT) {
            String className = type.getClassName();
            int lastDot = className.lastIndexOf('.');
            return lastDot == -1 ? className : className.substring(lastDot + 1);
        }
        return type.getClassName();
    }

    private static String repeat(int times) {
        StringBuilder sb = new StringBuilder("[]".length() * times);
        for (int i = 0; i < times; i++) {
            sb.append("[]");
        }
        return sb.toString();
    }
}
