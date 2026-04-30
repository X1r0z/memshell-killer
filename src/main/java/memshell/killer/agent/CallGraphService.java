package memshell.killer.agent;

import memshell.killer.core.CallGraphResult;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
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
        Class<?> clazz = ClassSearch.exact(instrumentation, className);
        byte[] bytes = ClassFileDumper.dump(instrumentation, clazz);
        return analyzeBytes(className, bytes);
    }

    public static CallGraphResult analyzeBytes(String className, byte[] bytes) {
        ClassNode classNode = new ClassNode();
        new ClassReader(bytes).accept(classNode, 0);

        Map<String, List<String>> edges = new LinkedHashMap<>();
        Set<String> declared = new HashSet<>();
        for (MethodNode method : classNode.methods) {
            if ((method.access & Opcodes.ACC_SYNTHETIC) == 0 && !isLifecycleMethod(method.name)) {
                declared.add(method.name);
            }
        }
        for (MethodNode method : classNode.methods) {
            if ((method.access & Opcodes.ACC_SYNTHETIC) != 0 || isLifecycleMethod(method.name)) {
                continue;
            }
            String caller = method.name;
            List<String> callees = new ArrayList<>();
            for (AbstractInsnNode node = method.instructions.getFirst(); node != null; node = node.getNext()) {
                if (node instanceof MethodInsnNode) {
                    MethodInsnNode call = (MethodInsnNode) node;
                    if ("<init>".equals(call.name) || "<clinit>".equals(call.name)) {
                        continue;
                    }
                    String callee;
                    if (classNode.name.equals(call.owner) && declared.contains(call.name)) {
                        callee = call.name;
                    } else {
                        String field = previousFieldName(node, classNode.name);
                        callee = field == null ? call.owner.replace('/', '.') + "." + call.name : field + "." + call.name;
                    }
                    if (!callees.contains(callee)) {
                        callees.add(callee);
                    }
                }
            }
            edges.put(caller, callees);
        }

        List<String> chains = new ArrayList<>();
        for (String caller : edges.keySet()) {
            buildChains(caller, caller, edges, new HashSet<>(), chains);
        }

        CallGraphResult result = new CallGraphResult();
        result.className = className;
        result.edges = edges;
        result.chains = chains;
        return result;
    }

    private static void buildChains(String root, String current, Map<String, List<String>> edges, Set<String> visiting, List<String> chains) {
        if (!visiting.add(current)) {
            chains.add(root + " -> " + current + " (cycle)");
            return;
        }
        List<String> callees = edges.get(current);
        if (callees == null || callees.isEmpty()) {
            if (!root.equals(current)) {
                chains.add(root);
            }
            visiting.remove(current);
            return;
        }
        for (String callee : callees) {
            if (edges.containsKey(callee)) {
                buildChains(root + " -> " + callee, callee, edges, visiting, chains);
            } else {
                chains.add(root + " -> " + callee);
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
}
