package memshell.killer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import memshell.killer.service.CallGraphService;
import memshell.killer.core.CallGraphResult;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class CallGraphServiceTest {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    @Test
    public void extractsInternalChainsAndFieldReceiverCalls() throws Exception {
        byte[] bytes = classBytes(Fixture.class);
        CallGraphResult result = CallGraphService.analyzeBytes(Fixture.class.getName(), bytes);

        Assert.assertTrue(result.edges.get("a").contains("b"));
        Assert.assertTrue(result.edges.get("b").contains("c"));
        Assert.assertTrue(result.chains.toString(), result.chains.contains("a -> b -> c"));
        Assert.assertTrue(result.edges.get("d").contains("e"));
        Assert.assertTrue(result.edges.get("d").contains("f.g"));
    }

    @Test
    public void serializesOriginalJsonFields() throws Exception {
        byte[] bytes = classBytes(Fixture.class);
        CallGraphResult result = CallGraphService.analyzeBytes(Fixture.class.getName(), bytes);

        String json = GSON.toJson(result);

        Assert.assertTrue(json, json.contains("\"className\""));
        Assert.assertTrue(json, json.contains("\"edges\""));
        Assert.assertTrue(json, json.contains("\"chains\""));
        Assert.assertTrue(json, json.contains("\"a\""));
    }

    private static byte[] classBytes(Class<?> type) throws Exception {
        String resource = "/" + type.getName().replace('.', '/') + ".class";
        InputStream in = type.getResourceAsStream(resource);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    static class Fixture {
        private Helper f = new Helper();

        void a() {
            b();
        }

        void b() {
            c();
        }

        void c() {
        }

        void d() {
            e();
            f.g();
        }

        void e() {
        }
    }

    static class Helper {
        void g() {
        }
    }
}
