---
name: "memshell-killer"
description: >
  Authorized Java Web memory shell detection, triage, decompilation, call-graph review, and in-memory route removal with the local memshell-killer CLI.
  Use whenever the user mentions Java Web MemShells, in-memory WebShells, suspicious Tomcat/Spring runtime registrations,
  JVM route dumping, Filter/Listener/Valve/Servlet/Controller/Interceptor inspection, loaded-class decompilation,
  suspicious class call tracing, or confirmed in-memory route removal from a running JVM.
  Prefer this skill over ad hoc shell commands for Java Web memshell incident response because it preserves evidence,
  uses structured CLI output, verifies removals, and keeps operational impact explicit.
allowed-tools: Bash(java:*), Bash(jq:*), Bash(jps:*), Bash(jcmd:*), Bash(ps:*)
---

# MemShell Killer

Use this skill for authorized Java Web incident response or administration when the user needs to inspect or remove live in-memory web routes from a running JVM. The bundled CLI attaches with a Java Agent, runs one operation, prints JSON, and detaches. It inspects current Tomcat and Spring Web MVC runtime registrations; it is not full host forensics, source-code scanning, persistence cleanup, or vulnerability remediation.

## Safety Boundary

Removal changes live application state. Preserve pre-removal evidence, explain why a route is malicious or risky, and ask for confirmation before removal unless the user has already clearly authorized it.

Do not treat a class name match alone as proof. Base conclusions on route metadata, classloader context, decompiled behavior, call evidence, and application-specific expectations. Use confidence labels such as `likely malicious`, `suspicious but needs confirmation`, or `probably benign`.

## Quick Command Chooser

| Question | Command |
|----------|---------|
| List every supported route registration | `dump <pid>` |
| List one route type | `dump --type filter <pid>` |
| Decompile a loaded class | `decompile --class-name <className> <pid>` |
| Decompile one method | `decompile --class-name <className> --method <methodName> <pid>` |
| Build local call evidence for a class | `call --class-name <className> <pid>` |
| Remove a confirmed route | `remove --type <type> --class-name <className> <pid>` |

## Usage

Run from the skill directory so the bundled jar in `scripts/` resolves:

```bash
cd /path/to/skills/memshell-killer
```

Use a full JDK when possible because attach support may be absent from a JRE.

When the user needs help finding the target JVM PID, prefer JDK tools such as `jps -lv` or `jcmd`.

```bash
jps -lv
jcmd
```

After selecting the target PID, run the CLI with that PID:

```bash
java -jar ./scripts/memshell-killer.jar <command> [options] <pid>
```

All commands return:

```json
{"success": true, "data": {}, "errors": []}
```

Check `.success` first. If it is `false`, report `.errors[]` and resolve the PID, attach permission, class lookup, JDK, or command issue before continuing.

## Output Formats

Default output is structured JSON.

Always prefer `jq` when reading CLI JSON output, especially for broad `dump`, large `decompile`, or noisy `call` results. Do not paste or inspect large raw JSON blobs when a focused `jq` filter can extract the fields needed for triage, evidence, removal, or verification. Use `jq` first to check `.success`, surface `.errors[]`, and reduce `.data` to the smallest useful shape before reasoning from the output.

Pipe through `jq` to reduce context and extract only what you need:

```bash
# Route inventory
java -jar ./scripts/memshell-killer.jar dump <pid> \
  | jq '.data | to_entries[] | select(.value | type == "array") | .value[] | {type, context, routes, name, className}'

# Broad or catch-all routes
java -jar ./scripts/memshell-killer.jar dump <pid> \
  | jq '.data | to_entries[] | select(.value | type == "array") | .value[] | select((.routes // []) | any(. == "/*" or . == "/**" or . == "/")) | {type, context, routes, name, className}'

# Decompiled source
java -jar ./scripts/memshell-killer.jar decompile --class-name com.example.SuspiciousFilter <pid> \
  | jq -r '.data.source'

# Call chains
java -jar ./scripts/memshell-killer.jar call --class-name com.example.SuspiciousFilter <pid> \
  | jq -r '.data.chains[]?'
```

Full JSON output schemas for each command: see `references/OUTPUT.md`.

## Commands

### `dump` - List live route registrations

```bash
java -jar ./scripts/memshell-killer.jar dump [--type <type>] <pid>
```

Supported route types: `filter`, `listener`, `valve`, `servlet`, `controller`, `interceptor`.

| Option | Description |
|--------|-------------|
| `<pid>` | Target JVM process ID |
| `--type` | Optional route type to inspect; omit it to inspect every supported type |

Start broad unless the user already knows the route type. Use focused dumps for confirmation and post-removal verification.

```bash
java -jar ./scripts/memshell-killer.jar dump <pid>
java -jar ./scripts/memshell-killer.jar dump --type filter <pid>
java -jar ./scripts/memshell-killer.jar dump --type interceptor <pid>
```

### `decompile` - Decompile a loaded class

```bash
java -jar ./scripts/memshell-killer.jar decompile --class-name <className> [--method <methodName>] <pid>
```

| Option | Description |
|--------|-------------|
| `<pid>` | Target JVM process ID |
| `--class-name` | Fully qualified loaded class name to decompile |
| `--method` | Optional method name to decompile instead of the full class |

Use full-class decompilation first. Narrow with `--method` only when route metadata or prior evidence points to one relevant handler method.

```bash
java -jar ./scripts/memshell-killer.jar decompile --class-name com.example.SuspiciousFilter <pid>
java -jar ./scripts/memshell-killer.jar decompile --class-name com.example.SuspiciousFilter --method doFilter <pid>
```

Summarize behavior instead of pasting excessive source. Highlight request triggers, command execution, dynamic class loading, reflection, script execution, network callbacks, file writes, and response-writing logic.

### `call` - Build local class call evidence

```bash
java -jar ./scripts/memshell-killer.jar call --class-name <className> <pid>
```

| Option | Description |
|--------|-------------|
| `<pid>` | Target JVM process ID |
| `--class-name` | Fully qualified loaded class name to analyze |

Use this when decompiled behavior is unclear or when you need concise evidence that request-handling methods reach suspicious sinks. The result is class-local call evidence, not complete whole-application data-flow proof.

```bash
java -jar ./scripts/memshell-killer.jar call --class-name com.example.SuspiciousFilter <pid>
```

### `remove` - Remove a confirmed route registration

```bash
java -jar ./scripts/memshell-killer.jar remove --type <type> --class-name <className> <pid>
```

Supported route types: `filter`, `listener`, `valve`, `servlet`, `controller`, `interceptor`.

| Option | Description |
|--------|-------------|
| `<pid>` | Target JVM process ID |
| `--type` | Route type from the dump result |
| `--class-name` | Exact implementation class from the dump result |

Use removal only for confirmed malicious registrations. Before running it, state the target PID, route type, class name, context, routes, and evidence. If the user has not clearly authorized removal, ask for confirmation.

```bash
java -jar ./scripts/memshell-killer.jar remove --type filter --class-name com.example.SuspiciousFilter <pid>
```

After removal, read `data.removed` and `data.details`, then rerun a focused `dump` to verify the route is gone. If `removed` is `0`, check route type, class name, classloader, context, framework wrappers/adapters/proxies, and whether the registration was already removed.

## Workflow

1. **Identify scope**: get the target PID from the user, or use `jps -lv` / `jcmd` when they ask for help finding it. Confirm the current user can attach to the process.
2. **Dump before changes**: run a broad `dump` unless the user already gave a route type. Use `jq` to extract only relevant route fields from the JSON output, then save or quote the important pre-removal fields.
3. **Triage routes**: prioritize unknown or recently loaded classes, broad patterns such as `/*` or `/**`, hidden paths, unusual classloaders, unexpected runtime registrations, and classes whose fields or methods suggest reflection, command execution, crypto/Base64 helpers, custom class loading, request header triggers, response writing, or network callbacks.
4. **Decompile candidates**: use `decompile --class-name`; narrow with `--method` only when a specific handler method is clearly relevant. Use `jq -r '.data.source'` for source extraction and summarize behavior rather than pasting excessive source.
5. **Review calls when useful**: use `call --class-name` when decompiled behavior is unclear or when you need concise evidence that request-handling methods reach suspicious sinks. Use `jq` to select only relevant chains or sinks. Treat it as local class-level evidence, not whole-application data-flow proof.
6. **Remove only confirmed malicious registrations**: before removal, state the PID, route type, class name, context, routes, and evidence. Use the exact `type` and `className` from the dump result.
7. **Verify**: rerun the same focused dump and confirm the suspicious class is absent. If it persists, consider duplicate registrations, wrong route type, multiple contexts, classloader mismatch, wrappers/adapters/proxies, or active reinjection.

## Evidence Signals

Suspicious signals are not automatic proof. Compare with known-good application routes when possible.

- Unexpected filters, listeners, valves, servlets, controllers, or interceptors registered at runtime.
- Catch-all or hidden routes unrelated to normal application behavior.
- Classloader, superclass, interface, field, or method metadata that does not match deployment expectations.
- Decompiled code that reads trigger headers/parameters, executes commands, defines classes, uses reflection/script engines, writes directly to responses, drops files, or calls out to the network.
- Call chains from request-handling methods to dangerous sinks such as `Runtime.exec`, `ProcessBuilder`, `defineClass`, `ScriptEngine`, reflective invocation, file writes, or outbound clients.

## Reporting

Keep reports short and evidence-led:

1. **Scope**: PID, commands run, and route types inspected.
2. **Findings**: `type`, `context`, `routes`, `name`, `className`, confidence label, and key signals for each suspicious entry.
3. **Code/call evidence**: decompiled behavior or call-chain highlights for inspected candidates.
4. **Action**: removed or not removed, exact remove command if used, and `removed` plus `details`.
5. **Verification**: post-removal dump result and remaining suspicious entries.

If nothing suspicious is found, say so directly and state the limit: this checked current in-memory Java Web registrations only, not dropped files, logs, source artifacts, compromised dependencies, persistence mechanisms, scheduled reinjection, or the original vulnerability. After successful removal, recommend containment separately: patch the exploited entry point, rotate affected secrets, review logs, scan deployment artifacts, and restart or redeploy from a trusted build when feasible.
