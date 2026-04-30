package memshell.killer.cli;

import memshell.killer.core.Jsons;
import memshell.killer.core.OperationRequest;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.concurrent.Callable;

@Command(name = "memshell-killer", mixinStandardHelpOptions = true, subcommands = {
        Main.DumpCommand.class, Main.JadCommand.class, Main.CallCommand.class, Main.RemoveCommand.class
})
public class Main implements Runnable {
    public static void main(String[] args) {
        int code = new CommandLine(new Main()).execute(args);
        System.exit(code);
    }

    @Override
    public void run() {
        new CommandLine(this).usage(System.out);
    }

    abstract static class AgentCommand implements Callable<Integer> {
        @Parameters(index = "0", description = "Target JVM pid")
        long pid;

        protected abstract OperationRequest request();

        @Override
        public Integer call() throws Exception {
            OperationRequest request = request();
            File result = File.createTempFile("memshell-killer-", ".json");
            request.resultPath = result.getAbsolutePath();
            String json = Jsons.GSON.toJson(request);
            String args = Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
            String jar = Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            AttachClient.loadAgent(pid, jar, args);
            byte[] bytes = Files.readAllBytes(result.toPath());
            System.out.println(new String(bytes, StandardCharsets.UTF_8));
            return 0;
        }
    }

    @Command(name = "dump", description = "Dump Java Web routes", mixinStandardHelpOptions = true)
    static class DumpCommand extends AgentCommand {
        @Option(names = "--type", description = "filter|listener|valve|servlet|controller|interceptor")
        String type;

        @Override
        protected OperationRequest request() {
            OperationRequest request = new OperationRequest();
            request.command = "dump";
            request.type = type == null ? "all" : type;
            return request;
        }
    }

    @Command(name = "jad", description = "Decompile a loaded class", mixinStandardHelpOptions = true)
    static class JadCommand extends AgentCommand {
        @Option(names = "--class-name", required = true)
        String className;

        @Option(names = "--method")
        String method;

        @Override
        protected OperationRequest request() {
            OperationRequest request = new OperationRequest();
            request.command = "jad";
            request.className = className;
            request.method = method;
            return request;
        }
    }

    @Command(name = "call", description = "Print file-level calls of a loaded class", mixinStandardHelpOptions = true)
    static class CallCommand extends AgentCommand {
        @Option(names = "--class-name", required = true)
        String className;

        @Override
        protected OperationRequest request() {
            OperationRequest request = new OperationRequest();
            request.command = "call";
            request.className = className;
            return request;
        }
    }

    @Command(name = "remove", description = "Remove a registered route by type and class", mixinStandardHelpOptions = true)
    static class RemoveCommand extends AgentCommand {
        @Option(names = "--type", required = true, description = "filter|listener|valve|servlet|controller|interceptor")
        String type;

        @Option(names = "--class-name", required = true)
        String className;

        @Override
        protected OperationRequest request() {
            OperationRequest request = new OperationRequest();
            request.command = "remove";
            request.type = type;
            request.className = className;
            return request;
        }
    }
}
