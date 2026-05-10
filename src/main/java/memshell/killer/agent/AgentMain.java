package memshell.killer.agent;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import memshell.killer.core.CommandRequest;
import memshell.killer.core.CommandResponse;
import memshell.killer.util.Reflects;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.instrument.Instrumentation;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

public class AgentMain {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    public static void agentmain(String args, Instrumentation instrumentation) {
        CommandRequest request = null;
        CommandResponse response;
        try {
            String json = new String(Base64.getUrlDecoder().decode(args), StandardCharsets.UTF_8);
            request = GSON.fromJson(json, CommandRequest.class);
            Throwable moduleAccessError = null;
            try {
                Reflects.bypassModule();
            } catch (Throwable t) {
                moduleAccessError = t;
            }
            response = new CommandHandler(instrumentation).handle(request);
            if (moduleAccessError != null) {
                response.errors.add(stackTrace(moduleAccessError));
            }
        } catch (Throwable t) {
            response = CommandResponse.error(stackTrace(t));
        }
        if (request != null && request.resultPath != null) {
            writeResult(request.resultPath, response);
        }
    }

    private static void writeResult(String path, CommandResponse response) {
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(Files.newOutputStream(Paths.get(path)), StandardCharsets.UTF_8))) {
            writer.print(GSON.toJson(response));
        } catch (Throwable t) {
            System.err.println("agent failed to write result to " + path);
            t.printStackTrace(System.err);
        }
    }

    private static String stackTrace(Throwable throwable) {
        java.io.StringWriter sw = new java.io.StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
