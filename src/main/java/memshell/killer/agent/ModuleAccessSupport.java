package memshell.killer.agent;

import memshell.killer.util.Reflects;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class ModuleAccessSupport {
    private ModuleAccessSupport() {
    }

    public static void bypassClassModule(Instrumentation instrumentation) {
        if (instrumentation == null) {
            return;
        }
        try {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            Field unsafeField = unsafeClass.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            Object unsafe = unsafeField.get(null);

            Object javaBaseModule = Class.class.getMethod("getModule").invoke(Object.class);
            Method objectFieldOffset = unsafe.getClass().getMethod("objectFieldOffset", Field.class);
            Object offset = objectFieldOffset.invoke(unsafe, Class.class.getDeclaredField("module"));
            Method getAndSetObject = unsafe.getClass().getMethod("getAndSetObject", Object.class, long.class, Object.class);
            getAndSetObject.invoke(unsafe, Reflects.class, ((Long) offset).longValue(), javaBaseModule);
        } catch (NoSuchMethodException ignored) {
            // Java 8 has no module system.
        } catch (Throwable t) {
            throw new IllegalStateException("failed to open java.base/java.lang to memshell-killer agent", t);
        }
    }
}
