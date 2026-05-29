# memshell-killer

A command-line tool for Java Web memory shell incident response.

It attaches to a running JVM via the Java Attach API, loads a Java Agent, runs a single operation, prints structured JSON, and detaches. With it you can inspect live runtime route registrations, decompile loaded classes, build class-local call evidence, and remove confirmed malicious registrations from memory — all without restarting the target application.

## Features

- **Route inventory** — enumerates live route registrations with classloader, superclass, interface, field, and method metadata.
- **On-the-fly decompilation** — decompiles a loaded class or single method straight from JVM bytecode, powered by CFR.
- **Call evidence** — traces class-local call chains from request-handling methods to suspicious sinks, powered by ASM.
- **In-memory removal** — removes a confirmed malicious registration by route type and class name, then verifies it is gone.
- **Structured output** — every command returns a `{success, data, errors}` JSON envelope, easy to pipe through `jq`.
- **Non-resident** — attaches by PID, runs one operation, and detaches; no agent stays loaded in the target.
- **Broad runtime support** — built for Java 8 and runs on JDK 17 (handles module-access restrictions automatically).

## Supported Route Types

| Type | Middleware | Description |
|------|------------|-------------|
| `filter` | Tomcat | Filter registrations and URL patterns |
| `listener` | Tomcat | Application and lifecycle listeners |
| `valve` | Tomcat | Pipeline valves |
| `servlet` | Tomcat | Servlet wrappers and mappings |
| `controller` | Spring Web MVC | Request mappings and URL handlers |
| `interceptor` | Spring Web MVC | Handler interceptors and mapped interceptors |

## Build

Build the shaded jar with Maven:

```bash
mvn clean package
```

Or build and copy the jar into the bundled skill's `scripts/` directory:

```bash
./build.sh
```

## Usage

Install the bundled skill so it can be used directly in Claude Code, Codex, and other agents:

```bash
npx skills add X1r0z/memshell-killer
```

Or invoke the CLI directly:

```bash
java -jar memshell-killer.jar <command> [options] <pid>
```

Find the target JVM PID with standard JDK tools:

```bash
jps -lv
jcmd
```

### dump

List live route registrations.

```bash
java -jar memshell-killer.jar dump [--type <type>] <pid>
```

| Option | Description |
|--------|-------------|
| `<pid>` | Target JVM process ID |
| `--type` | Optional route type (`filter`/`listener`/`valve`/`servlet`/`controller`/`interceptor`); omit to inspect all |

```bash
# All route types
java -jar memshell-killer.jar dump 12345

# Only filters
java -jar memshell-killer.jar dump --type filter 12345

# Surface broad / catch-all routes
java -jar memshell-killer.jar dump 12345 \
  | jq '.data[] | select((.routes // []) | any(. == "/*" or . == "/**" or . == "/")) | {type, context, routes, name, className}'
```

### decompile

Decompile a loaded class.

```bash
java -jar memshell-killer.jar decompile --class-name <className> [--method <methodName>] <pid>
```

| Option | Description |
|--------|-------------|
| `--class-name` | Fully qualified loaded class name |
| `--method` | Optional single method to decompile |

```bash
java -jar memshell-killer.jar decompile --class-name com.example.SuspiciousFilter 12345 \
  | jq -r '.data.source'
```

### call

Build class-local call evidence.

```bash
java -jar memshell-killer.jar call --class-name <className> <pid>
```

Use this when decompiled behavior is unclear or you need concise evidence that request-handling methods reach suspicious sinks (e.g. `Runtime.exec`, `ProcessBuilder`, `defineClass`, `ScriptEngine`). The result is class-local evidence, not whole-application data-flow proof.

```bash
java -jar memshell-killer.jar call --class-name com.example.SuspiciousFilter 12345 \
  | jq -r '.data.chains[]?'
```

### remove

Remove a confirmed route registration.

```bash
java -jar memshell-killer.jar remove --type <type> --class-name <className> <pid>
```

Use removal only for **confirmed** malicious registrations, with the exact `type` and `className` from a prior `dump`.

```bash
java -jar memshell-killer.jar remove --type filter --class-name com.example.SuspiciousFilter 12345
```

After removal, check `data.removed` and `data.details`, then rerun a focused `dump` to verify the route is gone. If `removed` is `0`, re-check the route type, class name, classloader, context, framework wrappers/proxies, or whether it was already removed.

## License

MIT
