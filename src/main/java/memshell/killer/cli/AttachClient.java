package memshell.killer.cli;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

public final class AttachClient {
    private AttachClient() {
    }

    public static void loadAgent(long pid, String agentJar, String args) throws Exception {
        Object vm = null;
        try {
            Class<?> vmClass = loadVirtualMachineClass();
            Method attach = vmClass.getMethod("attach", String.class);
            vm = attach.invoke(null, String.valueOf(pid));
            Method loadAgent = vmClass.getMethod("loadAgent", String.class, String.class);
            loadAgent.invoke(vm, agentJar, args);
        } finally {
            if (vm != null) {
                try {
                    vm.getClass().getMethod("detach").invoke(vm);
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private static Class<?> loadVirtualMachineClass() throws Exception {
        try {
            return Class.forName("com.sun.tools.attach.VirtualMachine");
        } catch (ClassNotFoundException ignored) {
            File toolsJar = findToolsJar();
            if (toolsJar != null) {
                URL[] urls = {toolsJar.toURI().toURL()};
                ClassLoader parent = AttachClient.class.getClassLoader();
                URLClassLoader loader = new URLClassLoader(urls, parent);
                return Class.forName("com.sun.tools.attach.VirtualMachine", true, loader);
            }
            throw new ClassNotFoundException("com.sun.tools.attach.VirtualMachine not found. "
                    + "Run memshell-killer with a JDK that contains the Attach API. "
                    + "For Java 8, use a JDK instead of a JRE and make sure JAVA_HOME points to it.");
        }
    }

    private static File findToolsJar() {
        String javaHome = System.getProperty("java.home");
        if (javaHome == null || javaHome.isEmpty()) {
            return null;
        }

        File home = new File(javaHome);
        File[] candidates = {
                new File(home, "lib/tools.jar"),
                new File(home, "../lib/tools.jar")
        };
        for (File candidate : candidates) {
            if (candidate.isFile()) {
                return candidate;
            }
        }
        return null;
    }
}
