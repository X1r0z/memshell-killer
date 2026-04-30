package memshell.killer.agent;

import memshell.killer.core.JadResult;
import org.benf.cfr.reader.api.CfrDriver;
import org.benf.cfr.reader.api.OutputSinkFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.instrument.Instrumentation;
import java.util.*;

public class JadService {
    private final Instrumentation instrumentation;

    public JadService(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
    }

    public JadResult decompile(String className, String methodName) throws Exception {
        Class<?> clazz = ClassSearch.exact(instrumentation, className);
        byte[] bytes = ClassFileDumper.dump(instrumentation, clazz);
        return decompileBytes(className, methodName, bytes);
    }

    static JadResult decompileBytes(String className, String methodName, byte[] bytes) throws Exception {
        File file = File.createTempFile(className.replace('.', '_'), ".class");
        try {
            try (FileOutputStream out = new FileOutputStream(file)) {
                out.write(bytes);
            }
            String source = decompileFile(file, methodName);
            JadResult result = new JadResult();
            result.className = className;
            result.method = methodName;
            result.source = source;
            return result;
        } finally {
            if (file.exists() && !file.delete()) {
                file.deleteOnExit();
            }
        }
    }

    private static String decompileFile(File file, String methodName) {
        final StringBuilder sb = new StringBuilder(8192);
        OutputSinkFactory sinkFactory = new OutputSinkFactory() {
            @Override
            public List<SinkClass> getSupportedSinks(SinkType sinkType, Collection<SinkClass> collection) {
                return Arrays.asList(SinkClass.STRING, SinkClass.DECOMPILED, SinkClass.EXCEPTION_MESSAGE);
            }

            @Override
            public <T> Sink<T> getSink(SinkType sinkType, SinkClass sinkClass) {
                return sinkable -> {
                    if (sinkType == SinkType.PROGRESS) {
                        return;
                    }
                    sb.append(sinkable);
                };
            }
        };
        Map<String, String> options = new HashMap<>();
        options.put("showversion", "false");
        if (methodName != null && !methodName.isEmpty()) {
            options.put("methodname", methodName);
        }
        new CfrDriver.Builder().withOptions(options).withOutputSink(sinkFactory).build()
                .analyse(Collections.singletonList(file.getAbsolutePath()));
        return sb.toString();
    }
}
