package memshell.killer.agent;

import java.lang.instrument.Instrumentation;

public final class ClassSearch {
    private ClassSearch() {
    }

    public static Class<?> exact(Instrumentation instrumentation, String className) {
        for (Class<?> clazz : instrumentation.getAllLoadedClasses()) {
            if (clazz.getName().equals(className)) {
                return clazz;
            }
        }
        throw new IllegalArgumentException("class not loaded: " + className);
    }
}
