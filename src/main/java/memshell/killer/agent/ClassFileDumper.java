package memshell.killer.agent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

public class ClassFileDumper implements ClassFileTransformer {
    private final Class<?> target;
    private byte[] bytes;

    private ClassFileDumper(Class<?> target) {
        this.target = target;
    }

    public static byte[] dump(Instrumentation instrumentation, Class<?> target) throws Exception {
        ClassFileDumper dumper = new ClassFileDumper(target);
        instrumentation.addTransformer(dumper, true);
        try {
            instrumentation.retransformClasses(target);
            if (dumper.bytes == null) {
                throw new IllegalStateException("failed to dump bytecode for " + target.getName());
            }
            return dumper.bytes;
        } finally {
            instrumentation.removeTransformer(dumper);
        }
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (classBeingRedefined == target) {
            bytes = classfileBuffer.clone();
        }
        return null;
    }
}
