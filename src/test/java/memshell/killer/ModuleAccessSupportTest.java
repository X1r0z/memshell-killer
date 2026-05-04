package memshell.killer;

import memshell.killer.agent.ModuleAccessSupport;
import org.junit.Test;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class ModuleAccessSupportTest {
    @Test
    public void nullInstrumentationIsNoop() {
        ModuleAccessSupport.bypassClassModule(null);
    }

    @Test
    public void instrumentationIsNotRequiredForUnsafeBypassPath() {
        Instrumentation instrumentation = (Instrumentation) Proxy.newProxyInstance(
                Instrumentation.class.getClassLoader(),
                new Class<?>[]{Instrumentation.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        throw new AssertionError("Instrumentation should not be used by the Unsafe bypass path");
                    }
                }
        );

        ModuleAccessSupport.bypassClassModule(instrumentation);
    }
}
