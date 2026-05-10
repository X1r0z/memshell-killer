# JSON Output Formats

All commands output structured JSON by default. Every response shares this envelope:

```json
{
  "success": true,
  "data": {},
  "errors": []
}
```

| Field | Type | Description |
|-------|------|-------------|
| `success` | boolean | Whether the command completed successfully inside the target JVM |
| `data` | object | Command-specific payload |
| `errors` | string[] | Error messages or stack traces when `success` is `false` |

Check `success` before reading command-specific fields. If `success` is `false`, treat `data` as absent or incomplete and report `errors`.

## `dump`

Lists live Tomcat and Spring Web MVC route registrations. `data` is an array of route records. Use each record's `type` field to filter specific route types.

```json
{
  "success": true,
  "data": [
    {
      "type": "filter",
      "context": "/app",
      "routes": ["/*"],
      "name": "suspiciousFilter",
      "className": "com.example.SuspiciousFilter",
      "classInfo": {
        "className": "com.example.SuspiciousFilter",
        "classLoader": "org.apache.catalina.loader.ParallelWebappClassLoader@1a2b3c",
        "superClass": "java.lang.Object",
        "interfaces": ["javax.servlet.Filter"],
        "fields": ["private java.lang.String headerName"],
        "methods": ["doFilter", "init", "destroy"]
      }
    },
    {
      "type": "controller",
      "context": "org.springframework.web.context.WebApplicationContext(/app)",
      "routes": ["/admin/shell"],
      "name": "shell",
      "className": "com.example.SuspiciousController",
      "classInfo": {
        "className": "com.example.SuspiciousController",
        "classLoader": "org.apache.catalina.loader.ParallelWebappClassLoader@1a2b3c",
        "superClass": "java.lang.Object",
        "interfaces": [],
        "fields": [],
        "methods": ["shell"]
      }
    }
  ],
  "errors": []
}
```

Supported route types:

| Key | Middleware | Description |
|-----|------------|-------------|
| `filter` | Tomcat | Filter registrations and URL patterns |
| `listener` | Tomcat | Application and lifecycle listeners |
| `valve` | Tomcat | Pipeline valves |
| `servlet` | Tomcat | Servlet wrappers and mappings |
| `controller` | Spring Web MVC | Request mappings and URL handlers |
| `interceptor` | Spring Web MVC | Handler interceptors and mapped interceptors |

Route entry fields:

| Field | Type | Description |
|-------|------|-------------|
| `type` | string | Route type |
| `context` | string | Application or container context |
| `routes` | string[] | URL patterns, route identifiers, or interceptor include patterns |
| `name` | string\|null | Registration name when available |
| `className` | string\|null | Implementation class |
| `classInfo` | object\|null | Loaded-class metadata when resolvable |

`classInfo` fields:

| Field | Type | Description |
|-------|------|-------------|
| `className` | string | Loaded class name |
| `classLoader` | string | Classloader identity |
| `superClass` | string\|null | Direct superclass |
| `interfaces` | string[] | Directly implemented interfaces |
| `fields` | string[] | Declared fields |
| `methods` | string[] | Declared methods |

Filter route records by type:

```bash
jq '.data[] | select(.type == "filter") | {type, context, routes, name, className}'
```

## `decompile`

Decompiles a loaded class, or one method when `--method` is provided.

```json
{
  "success": true,
  "data": {
    "className": "com.example.SuspiciousFilter",
    "method": "doFilter",
    "source": "public class SuspiciousFilter implements Filter { ... }"
  },
  "errors": []
}
```

| Field | Type | Description |
|-------|------|-------------|
| `className` | string | Class requested with `--class-name` |
| `method` | string\|null | Method requested with `--method`, or `null` for full-class decompilation |
| `source` | string | CFR decompiler output |

Extract source:

```bash
jq -r '.data.source'
```

## `call`

Builds local class-level method call evidence from the loaded class bytecode.

```json
{
  "success": true,
  "data": {
    "className": "com.example.SuspiciousFilter",
    "edges": {
      "doFilter(ServletRequest, ServletResponse, FilterChain): void": [
        "check(ServletRequest): boolean",
        "java.lang.Runtime.exec"
      ],
      "check(ServletRequest): boolean": ["javax.servlet.ServletRequest.getParameter"]
    },
    "chains": [
      "doFilter(ServletRequest, ServletResponse, FilterChain): void -> check(ServletRequest): boolean -> javax.servlet.ServletRequest.getParameter",
      "doFilter(ServletRequest, ServletResponse, FilterChain): void -> java.lang.Runtime.exec"
    ]
  },
  "errors": []
}
```

| Field | Type | Description |
|-------|------|-------------|
| `className` | string | Class requested with `--class-name` |
| `edges` | object | Map of readable method signature to direct callees |
| `chains` | string[] | Readable call chains as strings |

`call` is class-local evidence, not whole-application data-flow proof. Absence of a dangerous sink does not prove the class is benign.

Extract readable chains:

```bash
jq -r '.data.chains[]?'
```

## `remove`

Removes live route registrations that match the exact route type and class name.

```json
{
  "success": true,
  "data": {
    "type": "filter",
    "className": "com.example.SuspiciousFilter",
    "removed": 1,
    "details": ["/app filter suspiciousFilter"]
  },
  "errors": []
}
```

| Field | Type | Description |
|-------|------|-------------|
| `type` | string | Route type requested with `--type` |
| `className` | string | Class requested with `--class-name` |
| `removed` | int | Number of matching live registrations removed |
| `details` | string[] | Human-readable removal details |

If `removed` is `0`, no matching registration was removed. Re-check route type, class name, classloader, context, wrapper/proxy/adaptor behavior, and whether the route was already removed.

Extract removal outcome:

```bash
jq '{success, type: .data.type, className: .data.className, removed: .data.removed, details: .data.details, errors}'
```
