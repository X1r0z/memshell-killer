package memshell.killer.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import memshell.killer.core.DecompileResult;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;

public class DecompileServiceTest {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    @Test
    public void deletesDumpedClassFileAfterDecompile() throws Exception {
        String className = Fixture.class.getName();
        String prefix = className.replace('.', '_');
        deleteExistingTempClasses(prefix);

        DecompileResult result = DecompileService.decompileBytes(className, null, classBytes(Fixture.class));

        Assert.assertEquals(className, result.className);
        Assert.assertTrue(result.source.contains("hello"));
        Assert.assertEquals(0, countTempClasses(prefix));
    }

    @Test
    public void serializesOriginalJsonFields() throws Exception {
        String className = Fixture.class.getName();
        DecompileResult result = DecompileService.decompileBytes(className, "hello", classBytes(Fixture.class));

        String json = GSON.toJson(result);

        Assert.assertTrue(json, json.contains("\"className\""));
        Assert.assertTrue(json, json.contains("\"method\""));
        Assert.assertTrue(json, json.contains("\"source\""));
        Assert.assertTrue(json, json.contains("\"hello\""));
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

    private static void deleteExistingTempClasses(String prefix) {
        File[] files = tempClasses(prefix);
        for (File file : files) {
            if (file.isFile()) {
                file.delete();
            }
        }
    }

    private static int countTempClasses(String prefix) {
        return tempClasses(prefix).length;
    }

    private static File[] tempClasses(String prefix) {
        File dir = new File(System.getProperty("java.io.tmpdir"));
        File[] files = dir.listFiles((ignored, name) -> name.startsWith(prefix) && name.endsWith(".class"));
        return files == null ? new File[0] : files;
    }

    static class Fixture {
        String hello() {
            return "hello";
        }
    }
}
