package memshell.killer.agent;

import memshell.killer.core.Jsons;
import memshell.killer.core.OperationRequest;
import memshell.killer.core.OperationResponse;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.instrument.Instrumentation;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

public class AgentMain {
    public static void agentmain(String args, Instrumentation instrumentation) {
        OperationRequest request = null;
        OperationResponse response;
        try {
            String json = new String(Base64.getUrlDecoder().decode(args), StandardCharsets.UTF_8);
            request = Jsons.GSON.fromJson(json, OperationRequest.class);
            Throwable moduleAccessError = null;
            try {
                ModuleAccessSupport.bypassClassModule(instrumentation);
            } catch (Throwable t) {
                moduleAccessError = t;
            }
            response = new OperationDispatcher(instrumentation).dispatch(request);
            if (moduleAccessError != null) {
                response.errors.add(stackTrace(moduleAccessError));
            }
        } catch (Throwable t) {
            response = OperationResponse.error(stackTrace(t));
        }
        if (request != null && request.resultPath != null) {
            writeResult(request.resultPath, response);
        }
    }

    private static void writeResult(String path, OperationResponse response) {
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(Files.newOutputStream(Paths.get(path)), StandardCharsets.UTF_8))) {
            writer.print(Jsons.GSON.toJson(response));
        } catch (Throwable ignored) {
        }
    }

    private static String stackTrace(Throwable throwable) {
        java.io.StringWriter sw = new java.io.StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
