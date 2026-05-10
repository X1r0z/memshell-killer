package memshell.killer.inspect;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class ClassIntrospector {
    private ClassIntrospector() {
    }

    public static ClassInfo inspect(Class<?> clazz) {
        ClassInfo classInfo = new ClassInfo();
        classInfo.className = clazz.getName();
        ClassLoader loader = clazz.getClassLoader();
        classInfo.classLoader = loader == null ? "bootstrap" : loader.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(loader));
        Class<?> superClass = clazz.getSuperclass();
        classInfo.superClass = superClass == null ? null : superClass.getName();
        for (Class<?> iface : clazz.getInterfaces()) {
            classInfo.interfaces.add(iface.getName());
        }
        for (Field field : clazz.getDeclaredFields()) {
            classInfo.fields.add(Modifier.toString(field.getModifiers()) + " " + field.getType().getTypeName() + " " + field.getName());
        }
        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            classInfo.methods.add(signature(clazz.getSimpleName(), constructor.getParameterTypes(), null, constructor.getModifiers()));
        }
        for (Method method : clazz.getDeclaredMethods()) {
            classInfo.methods.add(signature(method.getName(), method.getParameterTypes(), method.getReturnType(), method.getModifiers()));
        }
        return classInfo;
    }

    private static String signature(String name, Class<?>[] parameterTypes, Class<?> returnType, int modifiers) {
        StringBuilder sb = new StringBuilder();
        String modifierText = Modifier.toString(modifiers);
        if (!modifierText.isEmpty()) {
            sb.append(modifierText).append(' ');
        }
        if (returnType != null) {
            sb.append(returnType.getTypeName()).append(' ');
        }
        sb.append(name).append('(');
        for (int i = 0; i < parameterTypes.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(parameterTypes[i].getTypeName());
        }
        sb.append(')');
        return sb.toString();
    }
}
