package memshell.killer.agent;

import memshell.killer.core.CommandRequest;
import memshell.killer.core.CommandResponse;
import memshell.killer.route.RouteRegistry;
import memshell.killer.service.CallGraphService;
import memshell.killer.service.DecompileService;
import memshell.killer.service.DumpService;
import memshell.killer.service.RemoveService;

import java.lang.instrument.Instrumentation;

public class CommandHandler {
    private final Instrumentation instrumentation;
    private final RouteRegistry registry = new RouteRegistry();

    public CommandHandler(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
    }

    public CommandResponse handle(CommandRequest request) {
        try {
            if ("dump".equals(request.command)) {
                return CommandResponse.ok(new DumpService(registry).dump(request.type));
            }
            if ("decompile".equals(request.command)) {
                return CommandResponse.ok(new DecompileService(instrumentation).decompile(request.className, request.method));
            }
            if ("call".equals(request.command)) {
                return CommandResponse.ok(new CallGraphService(instrumentation).analyze(request.className));
            }
            if ("remove".equals(request.command)) {
                return CommandResponse.ok(new RemoveService(registry).remove(request.type, request.className));
            }
            return CommandResponse.error("unknown command: " + request.command);
        } catch (Throwable t) {
            return CommandResponse.error(stackTrace(t));
        }
    }

    private static String stackTrace(Throwable throwable) {
        java.io.StringWriter sw = new java.io.StringWriter();
        throwable.printStackTrace(new java.io.PrintWriter(sw));
        return sw.toString();
    }
}
